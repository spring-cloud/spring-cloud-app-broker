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

package org.springframework.cloud.appbroker.logging.recent.endpoint;

import java.util.UUID;

import org.cloudfoundry.logcache.v1.Envelope;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.logging.LoggingUtils;
import org.springframework.cloud.appbroker.logging.recent.RecentLogsProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecentLogsController {

	private static final LogMessageComparator LOG_MESSAGE_COMPARATOR = new LogMessageComparator();

	private final RecentLogsProvider recentLogsProviders;

	public RecentLogsController(@Autowired(required = false) RecentLogsProvider recentLogsProviders) {
		this.recentLogsProviders = recentLogsProviders;
	}

	@RequestMapping("/logs/{serviceInstanceId}/recentlogs")
	public Mono<ResponseEntity<byte[]>> recentLogs(@PathVariable("serviceInstanceId") String serviceInstanceId) {
		final String multipartBoundary = UUID.randomUUID().toString();
		final HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, "multipart/mixed; boundary=" + multipartBoundary);
		return recentLogsProviders.getLogs(serviceInstanceId)
			.collectList()
			.doOnNext(envelopes -> envelopes.sort(LOG_MESSAGE_COMPARATOR))
			.map(envelopes -> {
				final MultipartEncoder multipart = new MultipartEncoder(multipartBoundary);
				for (Envelope message : envelopes) {
					var dropsondeEvent = LoggingUtils.convertLogCacheEnvelopeToDropsonde(message);
					multipart.append(org.cloudfoundry.dropsonde.events.Envelope.ADAPTER.encode(dropsondeEvent));
				}

				return new ResponseEntity<>(multipart.terminateAndGetBytes(), headers, HttpStatus.OK);
			});
	}

}
