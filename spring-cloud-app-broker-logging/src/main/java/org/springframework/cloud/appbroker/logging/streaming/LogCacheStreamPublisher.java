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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.LoggingUtils;

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
		return client.applicationsV2()
			.get(GetApplicationRequest.builder()
				.applicationId(applicationId)
				.build())
			.map(response -> response.getEntity().getName())
			.flatMapMany(appName -> {
				long initialStartTime = Instant.now().minus(5, ChronoUnit.SECONDS).toEpochMilli() * 1_000_000L;
				return logCacheClient.read(
						ReadRequest.builder()
							.sourceId(applicationId)
							.envelopeTypes(EnvelopeType.LOG)
							.startTime(initialStartTime)
							.build())
					.flatMapMany(initialResponse -> {
						AtomicLong lastTimestamp = new AtomicLong(
							initialResponse.getEnvelopes().getBatch().stream()
								.mapToLong(org.cloudfoundry.logcache.v1.Envelope::getTimestamp)
								.max()
								.orElse(initialStartTime)
						);

						Flux<Envelope> initialLogs = Flux.fromIterable(initialResponse.getEnvelopes().getBatch())
							.map(LoggingUtils::convertLogCacheEnvelopeToDropsonde);

						Flux<Envelope> polledLogs = Flux.interval(Duration.ofSeconds(1))
							.flatMap(tick -> logCacheClient.read(
									ReadRequest.builder()
										.sourceId(applicationId)
										.envelopeTypes(EnvelopeType.LOG)
										.startTime(lastTimestamp.get() + 1)
										.build())
								.flatMapMany(readResponse -> {
									long maxTimestamp = readResponse.getEnvelopes().getBatch().stream()
										.mapToLong(org.cloudfoundry.logcache.v1.Envelope::getTimestamp)
										.max()
										.orElse(lastTimestamp.get());

									lastTimestamp.set(maxTimestamp);

									return Flux.fromIterable(readResponse.getEnvelopes().getBatch())
										.map(LoggingUtils::convertLogCacheEnvelopeToDropsonde);
								}))
							.onErrorResume(error -> {
								LOG.error("Error during log polling: ", error);
								return Flux.empty();
							});

						return Flux.merge(initialLogs, polledLogs)
							.retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
							.doOnError(error -> LOG.error("Streaming error", error));
					});
			});
	}

}
