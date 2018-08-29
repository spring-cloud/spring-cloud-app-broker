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
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;

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
				"spring.cloud.appbroker.services[0].service-name=service1",
				"spring.cloud.appbroker.services[0].plan-name=service1-plan1",

				"spring.cloud.appbroker.services[0].apps[0].path=classpath:app1.jar",
				"spring.cloud.appbroker.services[0].apps[0].name=app1",
				"spring.cloud.appbroker.services[0].apps[0].properties.memory=1G",

				"spring.cloud.appbroker.services[0].apps[1].path=classpath:app2.jar",
				"spring.cloud.appbroker.services[0].apps[1].name=app2",
				"spring.cloud.appbroker.services[0].apps[1].properties.memory=2G",
				"spring.cloud.appbroker.services[0].apps[1].properties.instances=2",

				"spring.cloud.appbroker.services[1].service-name=service2",
				"spring.cloud.appbroker.services[1].plan-name=service2-plan1",

				"spring.cloud.appbroker.services[1].apps[0].path=classpath:app3.jar",
				"spring.cloud.appbroker.services[1].apps[0].name=app3",

				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.com",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret"
			)
			.run((context) -> {
				assertThat(context).hasSingleBean(BrokeredServices.class);
				BrokeredServices brokeredServices = context.getBean(BrokeredServices.class);
				assertThat(brokeredServices).hasSize(2);

				assertThat(brokeredServices.get(0).getServiceName()).isEqualTo("service1");
				assertThat(brokeredServices.get(0).getPlanName()).isEqualTo("service1-plan1");

				assertThat(brokeredServices.get(0).getApps().get(0).getName()).isEqualTo("app1");
				assertThat(brokeredServices.get(0).getApps().get(0).getPath()).isEqualTo("classpath:app1.jar");
				assertThat(brokeredServices.get(0).getApps().get(0).getProperties().get("memory")).isEqualTo("1G");
				assertThat(brokeredServices.get(0).getApps().get(0).getProperties().get("instances")).isNull();

				assertThat(brokeredServices.get(0).getApps().get(1).getName()).isEqualTo("app2");
				assertThat(brokeredServices.get(0).getApps().get(1).getPath()).isEqualTo("classpath:app2.jar");
				assertThat(brokeredServices.get(0).getApps().get(1).getProperties().get("memory")).isEqualTo("2G");
				assertThat(brokeredServices.get(0).getApps().get(1).getProperties().get("instances")).isEqualTo("2");

				assertThat(brokeredServices.get(1).getServiceName()).isEqualTo("service2");
				assertThat(brokeredServices.get(1).getPlanName()).isEqualTo("service2-plan1");

				assertThat(brokeredServices.get(1).getApps().get(0).getName()).isEqualTo("app3");
				assertThat(brokeredServices.get(1).getApps().get(0).getPath()).isEqualTo("classpath:app3.jar");

				assertThat(context).hasSingleBean(DeployerClient.class);
				assertThat(context).hasSingleBean(BackingAppDeploymentService.class);
				assertThat(context).hasSingleBean(ParametersTransformationService.class);
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