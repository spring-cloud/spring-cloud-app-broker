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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.extensions.credentials.CredHubCredentialsGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.SimpleCredentialGenerator;
import org.springframework.cloud.appbroker.workflow.binding.CredHubPersistingCreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.workflow.binding.CredHubPersistingDeleteServiceInstanceBindingWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.core.CredHubTemplate;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class CredHubAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			CloudFoundryAppDeployerAutoConfiguration.class,
			AppBrokerAutoConfiguration.class,
			CredHubAutoConfiguration.class
		))
		.withUserConfiguration(CredHubConfiguration.class)
		.withPropertyValues(
			"spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.com",
			"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
			"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
		);

	@Test
	void servicesAreNotCreatedWithoutCredHubOnClasspath() {
		contextRunner
			.withClassLoader(new FilteredClassLoader(CredHubOperations.class))
			.run((context) -> {
				assertThat(context)
					.hasSingleBean(CredentialGenerator.class)
					.getBean(CredentialGenerator.class)
					.isExactlyInstanceOf(SimpleCredentialGenerator.class);

				assertThat(context)
					.doesNotHaveBean(CredHubPersistingCreateServiceInstanceAppBindingWorkflow.class)
					.doesNotHaveBean(CredHubPersistingDeleteServiceInstanceBindingWorkflow.class);
			});
	}

	@Test
	void servicesAreCreatedWithCredHubConfigured() {
		contextRunner
			.run((context) -> {
				assertThat(context)
					.hasSingleBean(CredentialGenerator.class)
					.getBean(CredentialGenerator.class)
					.isExactlyInstanceOf(CredHubCredentialsGenerator.class);

				assertThat(context)
					.hasSingleBean(CredHubPersistingCreateServiceInstanceAppBindingWorkflow.class)
					.hasSingleBean(CredHubPersistingDeleteServiceInstanceBindingWorkflow.class);
			});
	}

	@TestConfiguration
	public static class CredHubConfiguration {

		@Bean
		public CredHubOperations credHubOperations() {
			return new CredHubTemplate(new RestTemplate());
		}

	}

}
