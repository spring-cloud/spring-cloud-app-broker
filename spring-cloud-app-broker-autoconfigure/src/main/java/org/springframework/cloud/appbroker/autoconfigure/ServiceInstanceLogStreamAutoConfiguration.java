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

package org.springframework.cloud.appbroker.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.logcache.v1.LogCacheClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.streaming.ApplicationLogStreamPublisher;
import org.springframework.cloud.appbroker.logging.streaming.LogCacheStreamPublisher;
import org.springframework.cloud.appbroker.logging.streaming.LogStreamPublisher;
import org.springframework.cloud.appbroker.logging.streaming.endpoint.StreamingLogWebSocketHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

@Configuration
@ConditionalOnClass(ApplicationLogStreamPublisher.class)
@ConditionalOnBean(ApplicationIdsProvider.class)
public class ServiceInstanceLogStreamAutoConfiguration {

	@Bean
	public StreamingLogWebSocketHandler streamingLogWebSocketHandler(
		ApplicationEventPublisher applicationEventPublisher) {
		return new StreamingLogWebSocketHandler(applicationEventPublisher);
	}

	@Bean
	@ConditionalOnMissingBean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}

	@Bean
	public HandlerMapping logsHandlerMapping(StreamingLogWebSocketHandler webSocketHandler) {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/logs/**", webSocketHandler);

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setUrlMap(map);
		return handlerMapping;
	}

	@Bean
	@ConditionalOnMissingBean
	public LogStreamPublisher<org.cloudfoundry.dropsonde.events.Envelope> streamLogsPublisher(
		CloudFoundryClient cloudFoundryClient,
		LogCacheClient logCacheClient,
		ApplicationIdsProvider applicationIdsProvider) {
		return new LogCacheStreamPublisher(cloudFoundryClient, logCacheClient, applicationIdsProvider);
	}

	@Bean
	public ApplicationLogStreamPublisher applicationLogsPublisher(LogStreamPublisher<org.cloudfoundry.dropsonde.events.Envelope> logStreamPublisher,
		ApplicationEventPublisher eventPublisher) {
		return new ApplicationLogStreamPublisher(logStreamPublisher, eventPublisher);
	}

}
