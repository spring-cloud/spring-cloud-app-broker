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

package org.springframework.cloud.appbroker.logging.streaming;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.example.streaming.LogStreamingTestApp;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.doppler.StreamRequest;
import org.cloudfoundry.dropsonde.events.Envelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.appbroker.logging.LoggingUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = LogStreamingTestApp.class)
class ServiceInstanceLogStreamingTest {

	@LocalServerPort
	int port;

	@MockBean
	DopplerClient dopplerClient;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	CloudFoundryClient cloudFoundryClient;

	private Envelope expectedEnvelope;

	private final AtomicReference<Envelope> actualEnvelope = new AtomicReference<>();

	private String serviceInstanceId;

	@BeforeEach
	void setUp() {
		serviceInstanceId = UUID.randomUUID().toString();
		String expectedTestMessage = "test message " + serviceInstanceId;

		org.cloudfoundry.doppler.LogMessage testMessage = org.cloudfoundry.doppler.LogMessage
			.builder()
			.message(expectedTestMessage)
			.timestamp(Instant.now().toEpochMilli())
			.messageType(MessageType.OUT).build();

		org.cloudfoundry.doppler.Envelope testEnvelope = org.cloudfoundry.doppler.Envelope
			.builder()
			.eventType(EventType.LOG_MESSAGE).origin("test")
			.logMessage(testMessage).build();

		// expectations are instances of Dropsnode Envelope + LogMessage
		Envelope envelope = LoggingUtils.convertDopplerEnvelopeToDropsonde(testEnvelope);
		org.cloudfoundry.dropsonde.events.LogMessage expectedMessage = new org.cloudfoundry.dropsonde.events.LogMessage.Builder()
			.message_type(envelope.logMessage.message_type)
			.source_instance("test-app null")
			.timestamp(envelope.logMessage.timestamp)
			.message(envelope.logMessage.message)
			.build();

		expectedEnvelope = new Envelope.Builder()
			.eventType(envelope.eventType)
			.logMessage(expectedMessage)
			.origin("test")
			.build();

		StreamRequest request = StreamRequest.builder().applicationId(LogStreamingTestApp.getAppId()).build();
		given(dopplerClient.stream(request)).willReturn(Flux.just(testEnvelope));
		given(cloudFoundryClient.applicationsV2().get(
			GetApplicationRequest.builder().applicationId(LogStreamingTestApp.getAppId()).build()))
			.willReturn(Mono.just(
				GetApplicationResponse.builder().entity(ApplicationEntity.builder().name("test-app").build()).build()));

	}

	@Test
	void shouldPublishWebSocketEndpoint() {
		Disposable subscription = connectToLogsStreamEndpoint();

		await().atMost(Duration.ofSeconds(1))
			.untilAsserted(() -> assertThat(actualEnvelope).hasValue(expectedEnvelope));

		subscription.dispose();
	}

	@Test
	void shouldPublishEventOnDisconnect() {
		Disposable subscription = connectToLogsStreamEndpoint();

		await().atMost(Duration.ofSeconds(1))
			.untilAsserted(() -> assertThat(actualEnvelope.get()).isNotNull());

		subscription.dispose();

		await().atMost(Duration.ofSeconds(1))
			.untilAsserted(() -> assertThat(LogStreamingTestApp.isReceivedStopEvent()).isTrue());
	}

	private Disposable connectToLogsStreamEndpoint() {
		URI uri = URI.create("ws://localhost:" + port + "/logs/" + serviceInstanceId + "/stream");

		WebSocketClient client = new ReactorNettyWebSocketClient();
		return client.execute(uri, getWebSocketHandler()).subscribe();
	}

	private WebSocketHandler getWebSocketHandler() {
		return session -> session
			.receive()
			.doOnNext(message -> {
				DataBuffer buffer = message.getPayload();
				try {
					actualEnvelope.set(Envelope.ADAPTER.decode(
						buffer.asInputStream()));
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			})
			.then();
	}

}
