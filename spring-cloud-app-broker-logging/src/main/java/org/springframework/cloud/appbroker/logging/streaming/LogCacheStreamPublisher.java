/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.logging.streaming;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.dropsonde.events.Envelope;
import org.cloudfoundry.logcache.v1.EnvelopeType;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.logcache.v1.ReadRequest;
import org.cloudfoundry.logcache.v1.ReadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.LoggingUtils;

/***
 * Class to Stream LocCache Envelopes using the same pattern as the
 * <a href="https://github.com/cloudfoundry/log-cache-cli/blob/main/internal/command/tail.go#L134">loc-cache-cli</a>
 */
public class LogCacheStreamPublisher implements LogStreamPublisher<Envelope> {

	private static final Logger LOG = LoggerFactory.getLogger(LogCacheStreamPublisher.class);

	private final CloudFoundryClient client;

	private final LogCacheClient logCacheClient;

	private final ApplicationIdsProvider applicationIdsProvider;

	public LogCacheStreamPublisher(
		CloudFoundryClient client,
		LogCacheClient logCacheClient,
		ApplicationIdsProvider applicationIdsProvider) {
		this.client = client;
		this.logCacheClient = logCacheClient;
		this.applicationIdsProvider = applicationIdsProvider;
	}

	@Override
	public Flux<Envelope> getLogStream(String serviceInstanceId) {
		return this.applicationIdsProvider
			.getApplicationIds(serviceInstanceId)
			.doOnNext(id -> LOG.debug("Starting log streaming for app with ID {}", id))
			.flatMap(this::createApplicationStreamer);
	}

	protected Flux<Envelope> createApplicationStreamer(String applicationId) {
		return getApplicationName(applicationId)
			.flatMapMany(appName -> {
				long initialStartTime = Instant.now().minus(5, ChronoUnit.SECONDS).toEpochMilli() * 1_000_000L;
				return readLogCache(applicationId, initialStartTime)
					.flatMapMany(initialResponse -> {
						AtomicLong lastTimestamp = getLastTimestamp(initialResponse, initialStartTime);
						Flux<Envelope> initialLogs = convertEnvelopesToDropsonde(initialResponse);
						Flux<Envelope> polledLogs = Flux.interval(Duration.ofSeconds(1))
							.flatMap(tick -> readLogCache(applicationId, lastTimestamp.get() + 1)
								.flatMapMany(readResponse -> {
									updateLastTimestampFromResponse(readResponse, lastTimestamp);
									return convertEnvelopesToDropsonde(readResponse);
								}))
							.onErrorResume(error -> {
								LOG.error("Error during log polling", error);
								return Flux.empty();
							});

						return Flux.merge(initialLogs, polledLogs)
							.retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
							.doOnError(error -> LOG.error("Streaming error", error));
					});
			});
	}

	private static AtomicLong getLastTimestamp(ReadResponse initialResponse, long initialStartTime) {
		return new AtomicLong(
			initialResponse.getEnvelopes().getBatch().stream()
				.mapToLong(org.cloudfoundry.logcache.v1.Envelope::getTimestamp)
				.max()
				.orElse(initialStartTime)
		);
	}

	private static Flux<Envelope> convertEnvelopesToDropsonde(ReadResponse readResponse) {
		return Flux.fromIterable(readResponse.getEnvelopes().getBatch())
			.map(LoggingUtils::convertLogCacheEnvelopeToDropsonde);
	}

	private Mono<String> getApplicationName(String applicationId) {
		return client.applicationsV2()
			.get(GetApplicationRequest.builder()
				.applicationId(applicationId)
				.build())
			.map(response -> response.getEntity().getName());
	}

	private Mono<ReadResponse> readLogCache(String applicationId, long lastTimestamp) {
		return logCacheClient.read(
			ReadRequest.builder()
				.sourceId(applicationId)
				.envelopeTypes(EnvelopeType.LOG)
				.startTime(lastTimestamp)
				.build());
	}

	private static void updateLastTimestampFromResponse(ReadResponse readResponse, AtomicLong lastTimestamp) {
		long maxTimestamp = readResponse.getEnvelopes().getBatch().stream()
			.mapToLong(org.cloudfoundry.logcache.v1.Envelope::getTimestamp)
			.max()
			.orElse(lastTimestamp.get());

		lastTimestamp.set(maxTimestamp);
	}

}
