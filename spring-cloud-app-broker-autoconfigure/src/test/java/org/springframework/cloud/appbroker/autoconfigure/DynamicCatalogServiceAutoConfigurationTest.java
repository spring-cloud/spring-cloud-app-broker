/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.List;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryDeploymentProperties;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicCatalogServiceAutoConfigurationTest {

	private static final Logger logger = LoggerFactory.getLogger(DynamicCatalogServiceAutoConfigurationTest.class);

	@Test
	void contextFailsWhenOptInButMissingDependencies() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DynamicCatalogServiceAutoConfiguration.class))
			.withSystemProperties(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
		});
	}

	@Test
	void dynamicServiceDontLoadWhenNoPropertiesSet() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				DynamicCatalogServiceAutoConfiguration.class,
				MockedDynamicCatalogDependenciesAutoConfiguration.class))
			.run(context -> {
				assertThat(context).doesNotHaveBean(DynamicCatalogServiceAutoConfiguration.class);
			});
	}

	@Test
	void dynamicServiceLoadWhenOptInProperty() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				SingleServiceDefinitionAnswerAutoConfig.class,
				DynamicCatalogServiceAutoConfiguration.class
			))
//			.withPropertyValues(DynamicCatalogProperties.OPT_IN_PROPERTY + "=true") //Not sure why this seems ignored
			.withSystemProperties(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		contextRunner.run(context -> {
			Catalog catalog = context.getBean(Catalog.class);
			assertThat(catalog.getServiceDefinitions()).isNotEmpty();
			BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
			assertThat(brokeredServices).isNotEmpty();

			assertThat(context).hasSingleBean(Catalog.class);
			assertThat(context).hasSingleBean(BrokeredServices.class);
		});
	}

	@Test
	void serviceDefinitionMapperPropertiesAreProperlyLoaded() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				SingleServiceDefinitionAnswerAutoConfig.class,
				DynamicCatalogServiceAutoConfiguration.class
			))
//			.withPropertyValues(DynamicCatalogProperties.OPT_IN_PROPERTY + "=true") //Not sure why this seems ignored
			.withSystemProperties(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true",
				ServiceDefinitionMapperProperties.PROPERTY_PREFIX
					+ServiceDefinitionMapperProperties.SUFFIX_PROPERTY_KEY+ "=suffix")
		;
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(ServiceDefinitionMapperProperties.class);
			ServiceDefinitionMapperProperties serviceDefinitionMapperProperties
				= context.getBean(ServiceDefinitionMapperProperties.class);
			assertThat(serviceDefinitionMapperProperties.getSuffix()).isEqualTo("suffix");
		});
	}

	@Test
	void catalogFetchingFailuresTriggersContextFailure() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				DynamicCatalogServiceAutoConfiguration.class,
				ThrowingExceptionServiceDefinitionAnswerAutoConfig.class))
			.withSystemProperties(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
		});
	}

	@Test
	void emptyCatalogFetchedTriggersContextFailure() {
		ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
				DynamicCatalogServiceAutoConfiguration.class,
				EmptyServiceDefinitionAnswerAutoConfig.class))
			.withSystemProperties(DynamicCatalogConstants.OPT_IN_PROPERTY + "=true");
		applicationContextRunner.run(context -> {
			assertThat(context).hasFailed();
		});
	}



	@Configuration
	static class MockedDynamicCatalogDependenciesAutoConfiguration {
		@Bean
		CloudFoundryDeploymentProperties defaultDeploymentProperties() {
			return mock(CloudFoundryDeploymentProperties.class);
		}
		@Bean
		CloudFoundryOperations operations() {
			return mock(CloudFoundryOperations.class);
		}
		@Bean
		CloudFoundryClient cloudFoundryClient() {
			return mock(CloudFoundryClient.class);
		}
		@Bean
		CloudFoundryTargetProperties targetProperties () {
			return mock(CloudFoundryTargetProperties.class);
		}
	}

	@Configuration
	static abstract class MockedDynamicServiceConfig {
		@Bean
		DynamicCatalogService dynamicCatalogService() {
			DynamicCatalogService dynamicCatalogService = mock(DynamicCatalogService.class);
			when(dynamicCatalogService.fetchServiceDefinitions()).thenReturn(this.serviceDefinitionsAnswer());
			return dynamicCatalogService;
		}

		protected abstract List<ServiceDefinition> serviceDefinitionsAnswer();

	}

	@Configuration
	static class SingleServiceDefinitionAnswerAutoConfig extends MockedDynamicServiceConfig {
		protected List<ServiceDefinition> serviceDefinitionsAnswer() {
			String serviceName = "serviceName";
			String planName = "planName";
			return asList(ServiceDefinition
				.builder()
				.id(serviceName + "-id")
				.name(serviceName)
				.plans(Plan.builder()
					.id(planName + "-id")
					.name(planName)
					.build())
				.build()
			);
		}
	}

	@Configuration
	static class EmptyServiceDefinitionAnswerAutoConfig extends MockedDynamicServiceConfig {
		protected List<ServiceDefinition> serviceDefinitionsAnswer() {
			return emptyList();
		}
	}

	@Configuration
	static class ThrowingExceptionServiceDefinitionAnswerAutoConfig  {
		@Bean
		DynamicCatalogService dynamicCatalogService() {
			DynamicCatalogService dynamicCatalogService = mock(DynamicCatalogService.class);
			when(dynamicCatalogService.fetchServiceDefinitions()).thenThrow(new RuntimeException("Injected exception " +
				"while fetching catalog "));
			return dynamicCatalogService;
		}
	}


}