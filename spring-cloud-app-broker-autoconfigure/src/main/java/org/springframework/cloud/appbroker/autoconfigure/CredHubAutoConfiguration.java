/*
 * Copyright 2002-2019 the original author or authors
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.appbroker.extensions.credentials.CredHubCredentialsGenerator;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.appbroker.workflow.binding.CredHubPersistingCreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.workflow.binding.CredHubPersistingDeleteServiceInstanceBindingWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.credhub.autoconfig.CredHubTemplateAutoConfiguration;
import org.springframework.credhub.core.ReactiveCredHubOperations;

/**
 * CredHub auto-configuration
 */
@Configuration
@AutoConfigureBefore(AppBrokerAutoConfiguration.class)
@AutoConfigureAfter(CredHubTemplateAutoConfiguration.class)
@ConditionalOnClass(ReactiveCredHubOperations.class)
@ConditionalOnBean(ReactiveCredHubOperations.class)
public class CredHubAutoConfiguration {

	@Value("${spring.application.name}")
	private String appName;

	/**
	 * Provide a {@link CreateServiceInstanceAppBindingWorkflow} bean
	 *
	 * @param credHubOperations the ReactiveCredHubOperations bean
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceAppBindingWorkflow credhubPersistingCreateServiceInstanceAppBindingWorkflow(
		ReactiveCredHubOperations credHubOperations) {
		return new CredHubPersistingCreateServiceInstanceAppBindingWorkflow(credHubOperations, appName);
	}

	/**
	 * Provide a {@link DeleteServiceInstanceBindingWorkflow} bean
	 *
	 * @param credHubOperations the ReactiveCredHubOperations bean
	 * @return the bean
	 */
	@Bean
	public DeleteServiceInstanceBindingWorkflow credhubPersistingDeleteServiceInstanceAppBindingWorkflow(
		ReactiveCredHubOperations credHubOperations) {
		return new CredHubPersistingDeleteServiceInstanceBindingWorkflow(credHubOperations, appName);
	}

	/**
	 * Provide a {@link CredHubCredentialsGenerator} bean
	 *
	 * @param credHubOperations the ReactiveCredHubOperations bean
	 * @return the bean
	 */
	@Bean
	public CredHubCredentialsGenerator credHubCredentialsGenerator(ReactiveCredHubOperations credHubOperations) {
		return new CredHubCredentialsGenerator(credHubOperations);
	}

}
