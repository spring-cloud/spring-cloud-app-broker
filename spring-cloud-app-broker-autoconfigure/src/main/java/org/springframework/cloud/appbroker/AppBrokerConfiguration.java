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

package org.springframework.cloud.appbroker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingAppProperties;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
//FIXME This appeared to be not being loading in test with conditional on property
//@ConditionalOnProperty("spring.cloud.app.broker.deploy")
public class AppBrokerConfiguration {
	@Bean
	@ConfigurationProperties("spring.cloud.app.broker.deploy")
	public BackingAppProperties backingAppProperties() {
		return new BackingAppProperties();
	}

	@Bean
	public DeployerClient deployerClient(ReactiveAppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	@Bean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new BackingAppDeploymentService(deployerClient);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(CreateServiceInstanceWorkflow createWorkflow) {
		return new WorkflowServiceInstanceService(createWorkflow);
	}

	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow(BackingAppProperties backingAppProperties,
																	   BackingAppDeploymentService backingAppDeploymentService) {
		return new CreateServiceInstanceWorkflow(backingAppProperties, backingAppDeploymentService);
	}
}
