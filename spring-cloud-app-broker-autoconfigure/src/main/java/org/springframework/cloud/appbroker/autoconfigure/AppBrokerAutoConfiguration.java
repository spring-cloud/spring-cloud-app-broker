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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DefaultBackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DefaultBackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderFactory;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.credentials.SimpleCredentialGenerator;
import org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityBasicAuthCredentialProviderFactory;
import org.springframework.cloud.appbroker.extensions.credentials.SpringSecurityOAuth2CredentialProviderFactory;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.EnvironmentMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.ParameterMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.PropertyMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.targets.ServiceInstanceGuidSuffix;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance;
import org.springframework.cloud.appbroker.extensions.targets.TargetFactory;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.manager.AppManager;
import org.springframework.cloud.appbroker.manager.BackingAppManagementService;
import org.springframework.cloud.appbroker.manager.ManagementClient;
import org.springframework.cloud.appbroker.oauth2.OAuth2Client;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceRouteBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceBindingService;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.appbroker.state.InMemoryServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.InMemoryServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentDeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentUpdateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(CloudFoundryAppDeployerAutoConfiguration.class)
@ConditionalOnBean(AppDeployer.class)
public class AppBrokerAutoConfiguration {

	private static final String PROPERTY_PREFIX = "spring.cloud.appbroker";

	@Bean
	public DeployerClient deployerClient(AppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	@Bean
	@ConditionalOnMissingBean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new DefaultBackingAppDeploymentService(deployerClient);
	}

	@Bean
	public ManagementClient managementClient(AppManager appManager) {
		return new ManagementClient(appManager);
	}

	@Bean
	public BackingAppManagementService backingAppManagementService(ManagementClient managementClient,
		AppDeployer appDeployer, BrokeredServices brokeredServices, TargetService targetService) {
		return new BackingAppManagementService(managementClient, appDeployer, brokeredServices, targetService);
	}

	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".services")
	public BrokeredServices brokeredServices() {
		return BrokeredServices.builder().build();
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceStateRepository.class)
	public ServiceInstanceStateRepository serviceInstanceStateRepository() {
		return new InMemoryServiceInstanceStateRepository();
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingStateRepository.class)
	public ServiceInstanceBindingStateRepository serviceInstanceBindingStateRepository() {
		return new InMemoryServiceInstanceBindingStateRepository();
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
	public ParameterMappingParametersTransformerFactory parameterMappingParametersTransformerFactory() {
		return new ParameterMappingParametersTransformerFactory();
	}

	@Bean
	public BackingApplicationsParametersTransformationService backingApplicationsParametersTransformationService(
		List<ParametersTransformerFactory<BackingApplication, ?>> transformers) {
		return new BackingApplicationsParametersTransformationService(transformers);
	}

	@Bean
	public BackingServicesParametersTransformationService backingServicesParametersTransformationService(
		List<ParametersTransformerFactory<BackingService, ?>> transformers) {
		return new BackingServicesParametersTransformationService(transformers);
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
	public SpringSecurityOAuth2CredentialProviderFactory springSecurityOAuth2CredentialProvider(CredentialGenerator credentialGenerator,
																								OAuth2Client oAuth2Client) {
		return new SpringSecurityOAuth2CredentialProviderFactory(credentialGenerator, oAuth2Client);
	}

	@Bean
	public CredentialProviderService credentialProviderService(List<CredentialProviderFactory<?>> providers) {
		return new CredentialProviderService(providers);
	}

	@Bean
	public SpacePerServiceInstance spacePerServiceInstance() {
		return new SpacePerServiceInstance();
	}

	@Bean
	public ServiceInstanceGuidSuffix serviceInstanceGuidSuffix() {
		return new ServiceInstanceGuidSuffix();
	}

	@Bean
	public TargetService targetService(List<TargetFactory<?>> targets) {
		return new TargetService(targets);
	}

	@Bean
	@ConditionalOnMissingBean
	public BackingServicesProvisionService backingServicesProvisionService(DeployerClient deployerClient) {
		return new DefaultBackingServicesProvisionService(deployerClient);
	}

	@Bean
	public CreateServiceInstanceWorkflow appDeploymentCreateServiceInstanceWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		CredentialProviderService credentialProviderService,
		TargetService targetService,
		BackingServicesProvisionService backingServicesProvisionService) {
		return new AppDeploymentCreateServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			backingServicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			credentialProviderService,
			targetService);
	}

	@Bean
	public UpdateServiceInstanceWorkflow appDeploymentUpdateServiceInstanceWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingServicesProvisionService backingServicesProvisionService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		TargetService targetService) {

		return new AppDeploymentUpdateServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			backingServicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			targetService);
	}

	@Bean
	public DeleteServiceInstanceWorkflow appDeploymentDeleteServiceInstanceWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingServicesProvisionService backingServicesProvisionService,
		CredentialProviderService credentialProviderService,
		TargetService targetService) {

		return new AppDeploymentDeleteServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			backingServicesProvisionService, credentialProviderService,
			targetService
		);
	}

	@Bean
	public WorkflowServiceInstanceService serviceInstanceService(ServiceInstanceStateRepository stateRepository,
																 List<CreateServiceInstanceWorkflow> createWorkflows,
																 List<DeleteServiceInstanceWorkflow> deleteWorkflows,
																 List<UpdateServiceInstanceWorkflow> updateWorkflows) {
		return new WorkflowServiceInstanceService(stateRepository, createWorkflows, deleteWorkflows, updateWorkflows);
	}

	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingService.class)
	public WorkflowServiceInstanceBindingService serviceInstanceBindingService(
		ServiceInstanceBindingStateRepository stateRepository,
		@Autowired(required = false) List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows,
		@Autowired(required = false) List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows,
		@Autowired(required = false) List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows) {
		return new WorkflowServiceInstanceBindingService(stateRepository,
			createServiceInstanceAppBindingWorkflows,
			createServiceInstanceRouteBindingWorkflows,
			deleteServiceInstanceBindingWorkflows);
	}
}
