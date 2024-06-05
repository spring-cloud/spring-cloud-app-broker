/*
 * Copyright 2016-2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.logging;


import java.util.Base64;

import okio.ByteString;
import org.cloudfoundry.dropsonde.events.Envelope;
import org.cloudfoundry.dropsonde.events.LogMessage;

public final class LoggingUtils {

	private LoggingUtils() {
	}

	public static Envelope convertLogCacheEnvelopeToDropsonde(org.cloudfoundry.logcache.v1.Envelope envelope) {
		final Envelope.Builder builder = new Envelope.Builder()
			.eventType(Envelope.EventType.LogMessage)
			.tags(envelope.getTags())
			.origin(getFromTags(envelope, "rep"))
			.timestamp(envelope.getTimestamp())
			.logMessage(new LogMessage.Builder()
				.message(ByteString.of(Base64.getDecoder().decode(envelope.getLog().getPayload())))
				.message_type(getMessageType(envelope))
				.timestamp(envelope.getTimestamp())
				.source_instance(envelope.getInstanceId())
				.source_type(getFromTags(envelope, "source_type"))
				.build());
		return builder.build();
	}

	private static String getFromTags(org.cloudfoundry.logcache.v1.Envelope envelope, String sourceTypeTag) {
		String sourceType = envelope.getTags().get(sourceTypeTag);
		if (sourceType == null) {
			sourceType = "";
		}
		return sourceType;
	}

	private static LogMessage.MessageType getMessageType(org.cloudfoundry.logcache.v1.Envelope envelope) {
		LogMessage.MessageType messageType;
		if ("ERR".equals(envelope.getLog().getType().getValue())) {
			messageType = LogMessage.MessageType.ERR;
		}
		else {
			messageType = LogMessage.MessageType.OUT;
		}
		return messageType;
	}

}
