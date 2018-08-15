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
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.appbroker.state.InMemoryServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.DeleteServiceInstanceWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(AppDeployerAutoConfiguration.class)
@ConditionalOnBean(ReactiveAppDeployer.class)
public class AppBrokerAutoConfiguration {

	private static final String PROPERTY_PREFIX = "spring.cloud.appbroker";

	@Bean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new BackingAppDeploymentService(deployerClient);
	}

	@Bean
	public DeployerClient deployerClient(ReactiveAppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".apps")
	public BackingApplications backingApplications() {
		return new BackingApplications();
	}

	@Bean
	public ServiceInstanceStateRepository serviceInstanceStateRepository() {
		return new InMemoryServiceInstanceStateRepository();
	}

	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow(BackingApplications backingApplications,
																	   BackingAppDeploymentService backingAppDeploymentService) {
		return new CreateServiceInstanceWorkflow(backingApplications, backingAppDeploymentService);
	}

	@Bean
	public DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow(BackingApplications backingApplications,
																	   BackingAppDeploymentService backingAppDeploymentService) {
		return new DeleteServiceInstanceWorkflow(backingApplications, backingAppDeploymentService);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(ServiceInstanceStateRepository stateRepository,
																 CreateServiceInstanceWorkflow createWorkflow,
																 DeleteServiceInstanceWorkflow deleteWorkflow) {
		return new WorkflowServiceInstanceService(stateRepository,createWorkflow, deleteWorkflow);
	}
}
