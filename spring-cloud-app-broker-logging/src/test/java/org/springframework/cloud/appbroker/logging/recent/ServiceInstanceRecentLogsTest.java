/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.UUID;

import com.example.recentlog.RecentLogsTestApp;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = RecentLogsTestApp.class)
class ServiceInstanceRecentLogsTest {

	@LocalServerPort
	int port;

	@MockBean
	DopplerClient dopplerClient;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	CloudFoundryClient cloudFoundryClient;

	private String expectedTestMessage;

	@BeforeEach
	void setUp() {
		expectedTestMessage = "test message " + UUID.randomUUID();

		RecentLogsRequest request = RecentLogsRequest.builder().applicationId(RecentLogsTestApp.getAppId()).build();
		LogMessage testMessage = LogMessage
			.builder()
			.message(expectedTestMessage)
			.timestamp(Instant.now().toEpochMilli())
			.messageType(MessageType.OUT).build();

		Envelope testEnvelope = Envelope
			.builder()
			.eventType(EventType.LOG_MESSAGE).origin("test")
			.logMessage(testMessage).build();

		given(dopplerClient.recentLogs(request))
			.willReturn(Flux.just(testEnvelope));

		given(cloudFoundryClient.applicationsV2()
			.get(GetApplicationRequest.builder().applicationId(RecentLogsTestApp.getAppId()).build()))
			.willReturn(Mono.just(
				GetApplicationResponse.builder().entity(ApplicationEntity.builder().name("test-app").build()).build()));
	}

	@Test
	void shouldFetchLogs() {
		String serviceInstanceId = UUID.randomUUID().toString();
		WebTestClient client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();

		client.get().uri("/logs/{serviceInstanceId}/recentlogs", serviceInstanceId)
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.value(Matchers.containsString(expectedTestMessage));
	}

}
