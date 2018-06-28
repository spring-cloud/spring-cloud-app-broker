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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@AutoConfigureAfter(AppDeployerAutoConfiguration.class)
@ConditionalOnBean(ReactiveAppDeployer.class)
public class AppBrokerAutoConfiguration {

	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.deploy";

	@Bean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new BackingAppDeploymentService(deployerClient);
	}

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX)
	public BackingAppProperties backingAppProperties() {
		return new BackingAppProperties();
	}

	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow(BackingAppProperties backingAppProperties,
																	   BackingAppDeploymentService backingAppDeploymentService) {
		return new CreateServiceInstanceWorkflow(backingAppProperties, backingAppDeploymentService);
	}

	@Bean
	public DeployerClient deployerClient(ReactiveAppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(CreateServiceInstanceWorkflow createWorkflow) {
		return new WorkflowServiceInstanceService(createWorkflow);
	}
}
