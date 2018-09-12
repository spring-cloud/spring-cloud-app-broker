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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.extensions.parameters.EnvironmentMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformerFactory;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.state.InMemoryServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.UpdateServiceInstanceWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(AppDeployerAutoConfiguration.class)
@ConditionalOnBean(AppDeployer.class)
public class AppBrokerAutoConfiguration {

	private static final String PROPERTY_PREFIX = "spring.cloud.appbroker";

	@Bean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new BackingAppDeploymentService(deployerClient);
	}

	@Bean
	public DeployerClient deployerClient(AppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".services")
	public BrokeredServices brokeredServices() {
		return new BrokeredServices();
	}

	@Bean
	public ServiceInstanceStateRepository serviceInstanceStateRepository() {
		return new InMemoryServiceInstanceStateRepository();
	}

	@Bean
	public EnvironmentMappingParametersTransformerFactory simpleMappingParametersTransformerFactory() {
		return new EnvironmentMappingParametersTransformerFactory();
	}

	@Bean
	public ParametersTransformationService parametersTransformerService(List<ParametersTransformerFactory<?>> transformers) {
		return new ParametersTransformationService(transformers);
	}

	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow(BrokeredServices brokeredServices,
																	   BackingAppDeploymentService backingAppDeploymentService,
																	   ParametersTransformationService parametersTransformationService) {
		return new CreateServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService, parametersTransformationService);
	}

	@Bean
	public DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow(BrokeredServices brokeredServices,
																	   BackingAppDeploymentService backingAppDeploymentService) {
		return new DeleteServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService);
	}

	@Bean UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow(BrokeredServices brokeredServices,
																	  BackingAppDeploymentService backingAppDeploymentService,
																	  ParametersTransformationService parametersTransformationService) {
		return new UpdateServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService, parametersTransformationService);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(ServiceInstanceStateRepository stateRepository,
																 CreateServiceInstanceWorkflow createWorkflow,
																 DeleteServiceInstanceWorkflow deleteWorkflow,
																 UpdateServiceInstanceWorkflow updateWorkflow) {
		return new WorkflowServiceInstanceService(stateRepository, createWorkflow, deleteWorkflow, updateWorkflow);
	}
}
