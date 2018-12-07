/*
 * Copyright 2016-2018 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.autoconfigure;

import org.springframework.cloud.appbroker.workflow.binding.CredHubPersistingCreateServiceInstanceAppBindingWorkflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.credhub.core.ReactiveCredHubOperations;

@Configuration
@AutoConfigureBefore(AppBrokerAutoConfiguration.class)
public class CredHubAutoConfiguration {

	@Value("${spring.application.name}")
	private String appName;

	@Bean
	@ConditionalOnBean(ReactiveCredHubOperations.class)
	public CreateServiceInstanceAppBindingWorkflow credhubPersistingCreateServiceInstanceAppBindingWorkflow(ReactiveCredHubOperations credHubOperations) {
		return new CredHubPersistingCreateServiceInstanceAppBindingWorkflow(credHubOperations, appName);
	}

}
