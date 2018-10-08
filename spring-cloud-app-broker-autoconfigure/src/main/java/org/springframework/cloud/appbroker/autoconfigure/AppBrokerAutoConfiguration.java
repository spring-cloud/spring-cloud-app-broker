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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderFactory;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.credentials.SimpleCredentialGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory;
import org.springframework.cloud.appbroker.extensions.parameters.EnvironmentMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.PropertyMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance;
import org.springframework.cloud.appbroker.extensions.targets.TargetFactory;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.state.InMemoryServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentDeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentUpdateServiceInstanceWorkflow;
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
		return BrokeredServices.builder().build();
	}

	@Bean
	public ServiceInstanceStateRepository serviceInstanceStateRepository() {
		return new InMemoryServiceInstanceStateRepository();
	}

	@Bean
	public EnvironmentMappingParametersTransformerFactory environmentMappingParametersTransformerFactory() {
		return new EnvironmentMappingParametersTransformerFactory();
	}

	@Bean
	public PropertyMappingParametersTransformerFactory propertyMappingParametersTransformerFactory() {
		return new PropertyMappingParametersTransformerFactory();
	}

	@Bean
	public ParametersTransformationService parametersTransformerService(List<ParametersTransformerFactory<?>> transformers) {
		return new ParametersTransformationService(transformers);
	}

	@ConditionalOnMissingBean(CredentialGenerator.class)
	@Bean
	public SimpleCredentialGenerator simpleCredentialGenerator() {
		return new SimpleCredentialGenerator();
	}

	@Bean
	public SpringSecurityBasicAuthCredentialProviderFactory springSecurityBasicAuthCredentialProvider(CredentialGenerator credentialGenerator) {
		return new SpringSecurityBasicAuthCredentialProviderFactory(credentialGenerator);
	}

	@Bean
	public CredentialProviderService credentialProviderService(List<CredentialProviderFactory<?>> providers) {
		return new CredentialProviderService(providers);
	}

	@Bean
	public SpacePerServiceInstance targetFactory() {
		return new SpacePerServiceInstance();
	}

	@Bean
	public TargetService targetService(List<TargetFactory<?>> targets) {
		return new TargetService(targets);
	}

	@Bean
	public CreateServiceInstanceWorkflow appDeploymentCreateServiceInstanceWorkflow(BrokeredServices brokeredServices,
																					BackingAppDeploymentService backingAppDeploymentService,
																					ParametersTransformationService parametersTransformationService,
																					CredentialProviderService credentialProviderService,
																					TargetService targetService) {
		return new AppDeploymentCreateServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService, parametersTransformationService, credentialProviderService, targetService);
	}

	@Bean
	public DeleteServiceInstanceWorkflow appDeploymentDeleteServiceInstanceWorkflow(BrokeredServices brokeredServices,
																					BackingAppDeploymentService backingAppDeploymentService,
																					CredentialProviderService credentialProviderService,
																					TargetService targetService) {
		return new AppDeploymentDeleteServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService, credentialProviderService, targetService);
	}

	@Bean
	public UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow(BrokeredServices brokeredServices,
																	   BackingAppDeploymentService backingAppDeploymentService,
																	   ParametersTransformationService parametersTransformationService,
																	   TargetService targetService) {
		return new AppDeploymentUpdateServiceInstanceWorkflow(brokeredServices, backingAppDeploymentService, parametersTransformationService, targetService);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(ServiceInstanceStateRepository stateRepository,
																 List<CreateServiceInstanceWorkflow> createWorkflows,
																 List<DeleteServiceInstanceWorkflow> deleteWorkflows,
																 List<UpdateServiceInstanceWorkflow> updateWorkflows) {
		return new WorkflowServiceInstanceService(stateRepository, createWorkflows, deleteWorkflows, updateWorkflows);
	}
}
