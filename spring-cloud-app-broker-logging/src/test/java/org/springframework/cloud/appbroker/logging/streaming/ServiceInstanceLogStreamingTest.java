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

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.example.recentlog.RecentLogsTestApp;
import com.example.streaming.LogStreamingTestApp;
import okio.ByteString;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationResponse;
import org.cloudfoundry.dropsonde.events.LogMessage;
import org.cloudfoundry.logcache.v1.Envelope;
import org.cloudfoundry.logcache.v1.EnvelopeBatch;
import org.cloudfoundry.logcache.v1.Log;
import org.cloudfoundry.logcache.v1.LogCacheClient;
import org.cloudfoundry.logcache.v1.LogType;
import org.cloudfoundry.logcache.v1.ReadRequest;
import org.cloudfoundry.logcache.v1.ReadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.appbroker.logging.streaming.events.ServiceInstanceLogEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = LogStreamingTestApp.class)
class ServiceInstanceLogStreamingTest {

	private static final String SERVICE_INSTANCE_ID = UUID.randomUUID().toString();

	@LocalServerPort
	private int port;

	@MockBean
	private LogCacheClient logCacheClient;

	@MockBean(answer = Answers.RETURNS_DEEP_STUBS)
	private CloudFoundryClient cloudFoundryClient;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	private org.cloudfoundry.dropsonde.events.Envelope expectedEnvelope;

	private final AtomicReference<Envelope> actualEnvelope = new AtomicReference<>();

	@BeforeEach
	void setUp() {
		String expectedTestMessage = "test message " + UUID.randomUUID();

		Envelope testEnvelope =
			Envelope.builder()
				.timestamp(Instant.now().toEpochMilli())
				.log(Log.builder()
					.payload(new String(Base64.getEncoder().encode(expectedTestMessage.getBytes())))
					.type(LogType.OUT)
					.build())
				.build();
		expectedEnvelope = new org.cloudfoundry.dropsonde.events.Envelope.Builder()
			.logMessage(new LogMessage.Builder()
				.message(ByteString.of(Base64.getEncoder().encode(expectedTestMessage.getBytes())))
				.message_type(LogMessage.MessageType.OUT)
				.timestamp(0L)
				.build())
			.origin("")
			.eventType(org.cloudfoundry.dropsonde.events.Envelope.EventType.LogMessage)
			.build();
		actualEnvelope.set(testEnvelope);

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
	void shouldPublishWebSocketEndpoint() {
		Disposable subscription = connectToLogsStreamEndpoint();

		await().untilAsserted(() -> assertThat(Base64.getDecoder().decode(actualEnvelope.get().getLog().getPayload()))
			.isEqualTo(Base64.getDecoder().decode(expectedEnvelope.logMessage.message.toByteArray())));

		subscription.dispose();
	}

	@Test
	void shouldPublishEventOnDisconnect() {
		Disposable subscription = connectToLogsStreamEndpoint();

		await().untilAsserted(() -> assertThat(actualEnvelope.get()).isNotNull());

		subscription.dispose();

		await().untilAsserted(() -> assertThat(LogStreamingTestApp.isReceivedStopEvent()).isTrue());
	}

	@Test
	void shouldStopStreamingIfNoClient() {
		// CLI plugin doesn't always handle disconnect gracefully, so sometimes it is possible that log stream is
		// being published but there is no listener. In this case log streaming should be stopped.

		Disposable subscription = connectToLogsStreamEndpoint();
		subscription.dispose();

		applicationEventPublisher.publishEvent(
			new ServiceInstanceLogEvent(this, SERVICE_INSTANCE_ID, expectedEnvelope));
		await().untilAsserted(
			() -> assertThat(LogStreamingTestApp.getReceivedStopEventServiceInstanceId()).isEqualTo(
				SERVICE_INSTANCE_ID));
	}

	private Disposable connectToLogsStreamEndpoint() {
		URI uri = URI.create("ws://localhost:" + port + "/logs/" + SERVICE_INSTANCE_ID + "/stream");

		WebSocketClient client = new ReactorNettyWebSocketClient();
		return client.execute(uri, getWebSocketHandler()).subscribe();
	}

	private WebSocketHandler getWebSocketHandler() {
		return session -> session
			.receive()
			.doOnNext(message -> {
				DataBuffer buffer = message.getPayload();
				actualEnvelope.set(Envelope.builder()
					.log(Log.builder().payload(new String(Base64.getDecoder().decode(buffer.toString()))).build())
					.build());
			})
			.then();
	}

}
