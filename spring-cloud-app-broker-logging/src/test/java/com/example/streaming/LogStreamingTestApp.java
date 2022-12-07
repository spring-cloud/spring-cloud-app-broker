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

package com.example.streaming;

import java.util.UUID;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.streaming.events.StopServiceInstanceLoggingEvent;
import org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration;
import org.springframework.cloud.servicebroker.autoconfigure.web.reactive.ServiceBrokerWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication(exclude = {
	ServiceBrokerAutoConfiguration.class,
	ServiceBrokerWebFluxAutoConfiguration.class
})
public class LogStreamingTestApp {

	private static final String APP_ID = UUID.randomUUID().toString();

	private static boolean receivedStopEvent;
	private static String receivedStopEventServiceInstanceId;

	public static String getAppId() {
		return APP_ID;
	}

	public static boolean isReceivedStopEvent() {
		return receivedStopEvent;
	}

	public static String getReceivedStopEventServiceInstanceId() {
		return receivedStopEventServiceInstanceId;
	}

	@Bean
	ApplicationIdsProvider applicationIdsProvider() {
		return serviceInstanceId -> Flux.just(APP_ID);
	}

	@EventListener
	public void onStop(StopServiceInstanceLoggingEvent stopServiceInstanceLoggingEvent) {
		receivedStopEventServiceInstanceId = stopServiceInstanceLoggingEvent.getServiceInstanceId();
		receivedStopEvent = true;
	}

}
