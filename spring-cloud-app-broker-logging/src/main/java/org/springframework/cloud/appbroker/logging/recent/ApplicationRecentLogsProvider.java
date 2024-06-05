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

package org.springframework.cloud.appbroker.logging.recent;

import java.time.Instant;

import org.cloudfoundry.logcache.v1.Envelope;
import org.cloudfoundry.logcache.v1.EnvelopeType;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.logcache.v1.ReadRequest;
import reactor.core.publisher.Flux;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;

public class ApplicationRecentLogsProvider implements RecentLogsProvider {

	private final LogCacheClient logCacheClient;

	private final ApplicationIdsProvider applicationIdsProvider;

	public ApplicationRecentLogsProvider(LogCacheClient logCacheClient,
		ApplicationIdsProvider applicationIdsProvider) {
		this.logCacheClient = logCacheClient;
		this.applicationIdsProvider = applicationIdsProvider;
	}

	@Override
	public Flux<Envelope> getLogs(String serviceInstanceId) {
		return this.applicationIdsProvider.getApplicationIds(serviceInstanceId)
			.flatMap(this::recentLogs);
	}

	protected Flux<Envelope> recentLogs(String applicationId) {
		return logCacheClient
			.read(ReadRequest.builder()
				.sourceId(applicationId)
				.descending(true)
				.envelopeTypes(EnvelopeType.LOG)
				.limit(1000)
				.startTime(Instant.MIN.getEpochSecond())
				.build())
			.flatMapMany(readResponse -> Flux.fromIterable(readResponse.getEnvelopes().getBatch()));
	}

}
