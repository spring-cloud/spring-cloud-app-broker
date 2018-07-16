/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;

import static org.assertj.core.api.Assertions.assertThat;

class AppBrokerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AppBrokerAutoConfiguration.class));

	@Test
	void servicesAreCreatedWithAppDeployerConfigured() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(AppDeployerAutoConfiguration.class,
				CloudFoundryClientAutoConfiguration.class))
			.withPropertyValues(
				"spring.cloud.appbroker.apps.backingApplications[0].path=classpath:demo.jar",
				"spring.cloud.appbroker.apps.backingApplications[0].name=app",
				"spring.cloud.appbroker.apps.backingApplications[1].path=classpath:demo.jar",
				"spring.cloud.appbroker.apps.backingApplications[1].name=app2",
				"spring.cloud.appbroker.deployer.cloudfoundry.apiHost=https://api.example.com",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(BackingApplications.class);
				BackingApplications backingApplications = context.getBean(BackingApplications.class);
				assertThat(backingApplications.getBackingApplications()).hasSize(2);
				assertThat(context).hasSingleBean(DeployerClient.class);
				assertThat(context).hasSingleBean(BackingAppDeploymentService.class);
				assertThat(context).hasSingleBean(WorkflowServiceInstanceService.class);
				assertThat(context).hasSingleBean(CreateServiceInstanceWorkflow.class);
			});
	}

	@Test
	void servicesAreNotCreatedWithoutDeployerConfiguration() {
		this.contextRunner
			.run((context) -> {
				assertThat(context).doesNotHaveBean(BackingApplications.class);
				assertThat(context).doesNotHaveBean(DeployerClient.class);
			});
	}

}