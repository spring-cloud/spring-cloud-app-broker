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

package org.springframework.cloud.appbroker.autoconfigure;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.streaming.ApplicationLogStreamPublisher;
import org.springframework.cloud.appbroker.logging.streaming.LogStreamPublisher;
import org.springframework.cloud.appbroker.logging.streaming.endpoint.StreamingLogWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceInstanceLogStreamAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			AppBrokerAutoConfiguration.class,
			CloudFoundryAppDeployerAutoConfiguration.class,
			ServiceInstanceLogStreamAutoConfiguration.class
		))
		.withPropertyValues(
			"spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.local",
			"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
			"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
		);

	@Test
	void servicesAreNotCreatedWhenPublisherIsNotConfigured() {
		contextRunner
			.withClassLoader(new FilteredClassLoader(ApplicationLogStreamPublisher.class))
			.withUserConfiguration(LoggingConfiguration.class)
			.run(context -> assertThat(context)
				.doesNotHaveBean(StreamingLogWebSocketHandler.class)
				.doesNotHaveBean(WebSocketHandlerAdapter.class)
				.doesNotHaveBean(HandlerMapping.class)
				.doesNotHaveBean(LogStreamPublisher.class)
				.doesNotHaveBean(ApplicationLogStreamPublisher.class));
	}

	@Test
	void servicesAreNotCreatedWhenLoggingIsNotConfigured() {
		contextRunner
			.run(context -> assertThat(context)
				.doesNotHaveBean(StreamingLogWebSocketHandler.class)
				.doesNotHaveBean(WebSocketHandlerAdapter.class)
				.doesNotHaveBean(HandlerMapping.class)
				.doesNotHaveBean(LogStreamPublisher.class)
				.doesNotHaveBean(ApplicationLogStreamPublisher.class));
	}

	@Test
	void servicesAreCreatedWithLoggingConfigured() {
		contextRunner
			.withUserConfiguration(LoggingConfiguration.class)
			.run(context -> assertThat(context)
				.hasSingleBean(StreamingLogWebSocketHandler.class)
				.hasSingleBean(WebSocketHandlerAdapter.class)
				.hasSingleBean(HandlerMapping.class)
				.hasSingleBean(LogStreamPublisher.class)
				.hasSingleBean(ApplicationLogStreamPublisher.class));
	}

	@TestConfiguration
	public static class LoggingConfiguration {

		@Bean
		public ApplicationIdsProvider applicationIdsProvider() {
			return serviceInstanceId -> Flux.just("app1");
		}

	}

}
