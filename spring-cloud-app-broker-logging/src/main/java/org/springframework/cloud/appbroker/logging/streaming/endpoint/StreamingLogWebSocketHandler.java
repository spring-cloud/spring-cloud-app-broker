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

package org.springframework.cloud.appbroker.logging.streaming.endpoint;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cloudfoundry.dropsonde.events.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Mono;

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

	private final ConcurrentHashMap<String, EmitterProcessor<Envelope>> processors = new ConcurrentHashMap<>();

	public StreamingLogWebSocketHandler(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		String serviceInstanceId = getServiceInstanceId(session);
		LOG.info("Connection established [{}}], service instance {}",
			session.getHandshakeInfo().getRemoteAddress(),
			serviceInstanceId);

		EmitterProcessor<Envelope> processor = processors
			.computeIfAbsent(serviceInstanceId, s -> EmitterProcessor.create());

		eventPublisher.publishEvent(new StartServiceInstanceLoggingEvent(this, serviceInstanceId));
		LOG.info("Published event to start streaming logs for service instance with ID {}", serviceInstanceId);

		return session
			.send(processor.map(envelope -> session
				.binaryMessage(dataBufferFactory -> dataBufferFactory.wrap(Envelope.ADAPTER.encode(envelope)))))
			.then()
			.doFinally(signalType -> afterConnectionClosed(session))
			.doOnError(throwable -> LOG
				.error("Error handling logging stream for service instance " + serviceInstanceId, throwable));
	}

	@Override
	public void onApplicationEvent(ServiceInstanceLogEvent event) {
		broadcastLogMessage(event);
	}

	public void broadcastLogMessage(ServiceInstanceLogEvent event) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received event to broadcast log message for " + event.getServiceInstanceId());
		}

		EmitterProcessor<Envelope> processor = this.processors.get(event.getServiceInstanceId());
		if (processor == null) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("No processor found for {}, can't stream logs", event.getServiceInstanceId());
			}

			return;
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Sending message to client for {}", event.getServiceInstanceId());
		}

		processor.onNext(event.getEnvelope());
	}

	private void afterConnectionClosed(WebSocketSession webSocketSession) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Connection closed [" + webSocketSession.getHandshakeInfo().getRemoteAddress() + "]");
		}

		final String serviceInstanceId = getServiceInstanceId(webSocketSession);
		eventPublisher.publishEvent(new StopServiceInstanceLoggingEvent(this, serviceInstanceId));
		processors.computeIfPresent(serviceInstanceId, (s, envelopeEmitterProcessor) -> null);
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
