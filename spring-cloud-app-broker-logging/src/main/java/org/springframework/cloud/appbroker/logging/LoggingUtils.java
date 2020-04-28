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

package org.springframework.cloud.appbroker.logging;

import okio.ByteString;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.MessageType;
import org.cloudfoundry.dropsonde.events.Envelope;
import org.cloudfoundry.dropsonde.events.LogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoggingUtils {

	private static final Logger LOG = LoggerFactory.getLogger(LoggingUtils.class);

	private LoggingUtils() {
	}

	public static Envelope convertDopplerEnvelopeToDropsonde(org.cloudfoundry.doppler.Envelope envelope) {
		final Envelope.Builder builder = new Envelope.Builder()
			.deployment(envelope.getDeployment())
			.eventType(toDropsondeEventType(envelope.getEventType()))
			.index(envelope.getIndex())
			.ip(envelope.getIp())
			.job(envelope.getJob())
			.origin(envelope.getOrigin())
			.tags(envelope.getTags())
			.timestamp(envelope.getTimestamp());

		if (envelope.getEventType() == EventType.LOG_MESSAGE) {
			final org.cloudfoundry.doppler.LogMessage logMessage = envelope.getLogMessage();
			if (LOG.isDebugEnabled()) {
				LOG.debug("Decoding message [" + logMessage.getTimestamp() + "]: " + logMessage.getMessage());
			}

			builder.logMessage(new LogMessage.Builder()
				.app_id(logMessage.getApplicationId())
				.message(ByteString.encodeUtf8(logMessage.getMessage()))
				.message_type(toDropsondeMessageType(logMessage.getMessageType()))
				.source_instance(logMessage.getSourceInstance())
				.source_type(logMessage.getSourceType())
				.timestamp(logMessage.getTimestamp())
				.build());
		}
		else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Unable to decode message of type " + envelope.getEventType());
			}
		}

		return builder.build();
	}

	public static org.cloudfoundry.doppler.Envelope injectAppNameIntoLogSourceInstance(String appName,
		org.cloudfoundry.doppler.Envelope envelope) {
		if (envelope.getEventType() != EventType.LOG_MESSAGE) {
			return envelope;
		}
		return org.cloudfoundry.doppler.Envelope.builder().from(envelope).logMessage(
			org.cloudfoundry.doppler.LogMessage.builder().from(envelope.getLogMessage())
				.sourceInstance(appName + " " + envelope.getLogMessage().getSourceInstance())
				.build()
		).build();
	}


	private static LogMessage.MessageType toDropsondeMessageType(MessageType messageType) {
		switch (messageType) {
			case ERR:
				return LogMessage.MessageType.ERR;
			case OUT:
				return LogMessage.MessageType.OUT;
			default:
				throw new IllegalArgumentException("Unknown message type " + messageType);
		}
	}

	private static Envelope.EventType toDropsondeEventType(EventType eventType) {
		switch (eventType) {
			case ERROR:
				return Envelope.EventType.Error;
			case CONTAINER_METRIC:
				return Envelope.EventType.ContainerMetric;
			case COUNTER_EVENT:
				return Envelope.EventType.CounterEvent;
			case HTTP_START_STOP:
				return Envelope.EventType.HttpStartStop;
			case LOG_MESSAGE:
				return Envelope.EventType.LogMessage;
			case VALUE_METRIC:
				return Envelope.EventType.ValueMetric;
		}
		throw new IllegalArgumentException("Unknown event type: " + eventType);
	}

}
