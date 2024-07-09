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

package org.springframework.cloud.appbroker.logging.streaming.endpoint;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.cloudfoundry.dropsonde.events.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.cloud.appbroker.logging.streaming.events.ServiceInstanceLogEvent;
import org.springframework.cloud.appbroker.logging.streaming.events.StartServiceInstanceLoggingEvent;
import org.springframework.cloud.appbroker.logging.streaming.events.StopServiceInstanceLoggingEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriTemplate;

public class StreamingLogWebSocketHandler implements WebSocketHandler, ApplicationListener<ServiceInstanceLogEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(StreamingLogWebSocketHandler.class);

	private static final UriTemplate LOGGING_URI_TEMPLATE = new UriTemplate("/logs/{serviceInstanceId}/stream");

	private final ApplicationEventPublisher eventPublisher;

	private final ConcurrentMap<String, Sinks.Many<Envelope>> envelopeSinks = new ConcurrentHashMap<>();

	public StreamingLogWebSocketHandler(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String serviceInstanceId = getServiceInstanceId(session);
		LOG.info("Connection established [{}], service instance {}",
			session.getHandshakeInfo().getRemoteAddress(),
			serviceInstanceId);

		Sinks.Many<Envelope> envelopeSink = envelopeSinks
			.computeIfAbsent(serviceInstanceId, s -> Sinks.many().multicast().onBackpressureBuffer());

		eventPublisher.publishEvent(new StartServiceInstanceLoggingEvent(this, serviceInstanceId));
		LOG.info("Published event to start streaming logs for service instance with ID {}", serviceInstanceId);

		return session.send(envelopeSink.asFlux()
				.map(envelope -> session.binaryMessage(
					dataBufferFactory -> dataBufferFactory.wrap(Envelope.ADAPTER.encode(envelope)))))
			.doFinally(signalType -> afterConnectionClosed(session, serviceInstanceId))
			.doOnError(throwable -> LOG.error(String.format("Error handling logging stream for service instance %s",
				serviceInstanceId), throwable));
	}

	@Override
	public void onApplicationEvent(ServiceInstanceLogEvent event) {
		broadcastLogMessage(event);
	}

	public void broadcastLogMessage(ServiceInstanceLogEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received event to broadcast log message for {}", event.getServiceInstanceId());
		}

		Sinks.Many<Envelope> envelopeSink = this.envelopeSinks.get(event.getServiceInstanceId());
		if (envelopeSink == null) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("No sink found for {}, stopping log streaming", event.getServiceInstanceId());
			}

			eventPublisher.publishEvent(new StopServiceInstanceLoggingEvent(this, event.getServiceInstanceId()));
			return;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending message to client for {}", event.getServiceInstanceId());
		}

		envelopeSink.tryEmitNext(event.getEnvelope()).orThrow();
	}

	private void afterConnectionClosed(WebSocketSession webSocketSession, String serviceInstanceId) {
		LOG.info("Connection closed [{}], service instance {}", webSocketSession.getHandshakeInfo().getRemoteAddress(),
			serviceInstanceId);

		eventPublisher.publishEvent(new StopServiceInstanceLoggingEvent(this, serviceInstanceId));

		Sinks.Many<Envelope> sink = envelopeSinks.remove(serviceInstanceId);
		if (sink != null) {
			sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
		}
	}

	private String getServiceInstanceId(WebSocketSession webSocketSession) {
		URI uri = webSocketSession.getHandshakeInfo().getUri();
		final Map<String, String> match = LOGGING_URI_TEMPLATE.match(uri.getPath());
		if (match.isEmpty()) {
			throw new ServiceInstanceNotFoundException();
		}

		return match.get("serviceInstanceId");
	}

}
