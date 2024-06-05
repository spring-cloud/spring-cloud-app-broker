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
import java.util.Base64;
import java.util.UUID;

import com.example.recentlog.RecentLogsTestApp;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.logcache.v1.Envelope;
import org.cloudfoundry.logcache.v1.EnvelopeBatch;
import org.cloudfoundry.logcache.v1.Log;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.logcache.v1.LogType;
import org.cloudfoundry.logcache.v1.ReadRequest;
import org.cloudfoundry.logcache.v1.ReadResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = RecentLogsTestApp.class)
class ServiceInstanceRecentLogsTest {

	@LocalServerPort
	private int port;

	@MockBean
	private LogCacheClient logCacheClient;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	private CloudFoundryClient cloudFoundryClient;

	private String expectedTestMessage;

	private static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();

	@BeforeEach
	void setUp() {
		expectedTestMessage = "test message " + UUID.randomUUID();

		Envelope testEnvelope =
			Envelope.builder()
				.timestamp(Instant.now().toEpochMilli())
				.log(Log.builder()
					.payload(new String(Base64.getEncoder().encode(expectedTestMessage.getBytes())))
					.type(LogType.OUT)
					.build())
				.build();

		ReadResponse response =
			ReadResponse.builder()
				.envelopes(EnvelopeBatch.builder().batch(testEnvelope).build())
				.build();

		given(logCacheClient.read(any(ReadRequest.class)))
			.willReturn(Mono.just(response));

		given(cloudFoundryClient.applicationsV2()
			.get(GetApplicationRequest.builder().applicationId(RecentLogsTestApp.getAppId()).build()))
			.willReturn(Mono.just(
				GetApplicationResponse.builder().entity(ApplicationEntity.builder().name("test-app").build()).build()));
	}

	@Test
	void shouldFetchLogs() {
		WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

		client.get().uri("/logs/{serviceInstanceId}/recentlogs", SERVICE_INSTANCE_ID)
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.value(Matchers.containsString(expectedTestMessage));
	}

}
