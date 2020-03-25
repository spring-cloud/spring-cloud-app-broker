/*
 * Copyright 2002-2020 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DefaultBackingAppDeploymentService;
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
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * App Broker Auto-configuration
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
@Configuration
@AutoConfigureAfter(CloudFoundryAppDeployerAutoConfiguration.class)
@ConditionalOnBean(AppDeployer.class)
public class AppBrokerAutoConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(AppBrokerAutoConfiguration.class);

	private static final String PROPERTY_PREFIX = "spring.cloud.appbroker";

	/**
	 * Provide a {@link DeployerClient} bean
	 *
	 * @param appDeployer the AppDeployer bean
	 * @return the bean
	 */
	@Bean
	public DeployerClient deployerClient(AppDeployer appDeployer) {
		return new DeployerClient(appDeployer);
	}

	/**
	 * Provide a {@link BackingAppDeploymentService} bean
	 *
	 * @param deployerClient the DeployerClient bean
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public BackingAppDeploymentService backingAppDeploymentService(DeployerClient deployerClient) {
		return new DefaultBackingAppDeploymentService(deployerClient);
	}

	/**
	 * Provide a {@link ManagementClient} bean
	 *
	 * @param appManager the AppManager bean
	 * @return the bean
	 */
	@Bean
	public ManagementClient managementClient(AppManager appManager) {
		return new ManagementClient(appManager);
	}

	/**
	 * Provide a {@link BackingAppManagementService} bean
	 *
	 * @param managementClient the ManagementClient bean
	 * @param appDeployer the AppDeployer bean
	 * @param brokeredServices the BrokeredServices bean
	 * @param targetService the TargetService bean
	 * @return the bean
	 */
	@Bean
	public BackingAppManagementService backingAppManagementService(ManagementClient managementClient,
		AppDeployer appDeployer, BrokeredServices brokeredServices, TargetService targetService) {
		return new BackingAppManagementService(managementClient, appDeployer, brokeredServices, targetService);
	}

	/**
	 * Provide a {@link BrokeredServices} bean
	 *
	 * @return the bean
	 */
	@Bean
	@ConfigurationProperties(PROPERTY_PREFIX + ".services")
	@ConditionalOnMissingBean
	public BrokeredServices brokeredServices() {
		return BrokeredServices.builder().build();
	}

	/**
	 * Provide a {@link ServiceInstanceStateRepository} bean
	 *
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean(ServiceInstanceStateRepository.class)
	public ServiceInstanceStateRepository serviceInstanceStateRepository() {
		if (LOG.isWarnEnabled()) {
			LOG.warn("The InMemoryServiceInstanceStateRepository is provided for demonstration and testing purposes " +
					"only. It is not suitable for production applications!");
		}
		return new InMemoryServiceInstanceStateRepository();
	}

	/**
	 * Provide a {@link ServiceInstanceBindingStateRepository} bean
	 *
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingStateRepository.class)
	public ServiceInstanceBindingStateRepository serviceInstanceBindingStateRepository() {
		if (LOG.isWarnEnabled()) {
			LOG.warn("The InMemoryServiceInstanceBindingStateRepository is provided for demonstration and testing " +
					"purposes only. It is not suitable for production applications!");
		}
		return new InMemoryServiceInstanceBindingStateRepository();
	}

	/**
	 * Provide an {@link EnvironmentMappingParametersTransformerFactory} bean
	 *
	 * @return the bean
	 */
	@Bean
	public EnvironmentMappingParametersTransformerFactory environmentMappingParametersTransformerFactory() {
		return new EnvironmentMappingParametersTransformerFactory();
	}

	/**
	 * Provide a {@link ParameterMappingParametersTransformerFactory} bean
	 *
	 * @return the bean
	 */
	@Bean
	public PropertyMappingParametersTransformerFactory propertyMappingParametersTransformerFactory() {
		return new PropertyMappingParametersTransformerFactory();
	}

	/**
	 * Provide a {@link ParameterMappingParametersTransformerFactory} bean
	 *
	 * @return the bean
	 */
	@Bean
	public ParameterMappingParametersTransformerFactory parameterMappingParametersTransformerFactory() {
		return new ParameterMappingParametersTransformerFactory();
	}

	/**
	 * Provide a {@link BackingApplicationsParametersTransformationService} bean
	 *
	 * @param transformers a collection of parameter transformers
	 * @return the bean
	 */
	@Bean
	public BackingApplicationsParametersTransformationService backingApplicationsParametersTransformationService(
		List<ParametersTransformerFactory<BackingApplication, ?>> transformers) {
		return new BackingApplicationsParametersTransformationService(transformers);
	}

	/**
	 * Provide a {@link BackingServicesParametersTransformationService} bean
	 *
	 * @param transformers a collection of parameter transformers
	 * @return the bean
	 */
	@Bean
	public BackingServicesParametersTransformationService backingServicesParametersTransformationService(
		List<ParametersTransformerFactory<BackingService, ?>> transformers) {
		return new BackingServicesParametersTransformationService(transformers);
	}

	/**
	 * Provide a {@link SimpleCredentialGenerator} bean
	 *
	 * @return the bean
	 */
	@ConditionalOnMissingBean(CredentialGenerator.class)
	@Bean
	public SimpleCredentialGenerator simpleCredentialGenerator() {
		return new SimpleCredentialGenerator();
	}

	/**
	 * Provide a {@link SpringSecurityBasicAuthCredentialProviderFactory} bean
	 *
	 * @param credentialGenerator the CredentialGenerator bean
	 * @return the bean
	 */
	@Bean
	public SpringSecurityBasicAuthCredentialProviderFactory springSecurityBasicAuthCredentialProvider(
		CredentialGenerator credentialGenerator) {
		return new SpringSecurityBasicAuthCredentialProviderFactory(credentialGenerator);
	}

	/**
	 * Provide a {@link SpringSecurityOAuth2CredentialProviderFactory} bean
	 *
	 * @param credentialGenerator the CredentialGenerator bean
	 * @param oAuth2Client the OAuth2Client bean
	 * @return the bean
	 */
	@Bean
	public SpringSecurityOAuth2CredentialProviderFactory springSecurityOAuth2CredentialProvider(
		CredentialGenerator credentialGenerator,
		OAuth2Client oAuth2Client) {
		return new SpringSecurityOAuth2CredentialProviderFactory(credentialGenerator, oAuth2Client);
	}

	/**
	 * Provide a {@link CredentialProviderService} bean
	 *
	 * @param providers a collection of credential providers
	 * @return the bean
	 */
	@Bean
	public CredentialProviderService credentialProviderService(List<CredentialProviderFactory<?>> providers) {
		return new CredentialProviderService(providers);
	}

	/**
	 * Provide a {@link SpacePerServiceInstance} bean
	 *
	 * @return the bean
	 */
	@Bean
	public SpacePerServiceInstance spacePerServiceInstance() {
		return new SpacePerServiceInstance();
	}

	/**
	 * Provide a {@link ServiceInstanceGuidSuffix} bean
	 *
	 * @return the bean
	 */
	@Bean
	public ServiceInstanceGuidSuffix serviceInstanceGuidSuffix() {
		return new ServiceInstanceGuidSuffix();
	}

	/**
	 * Provide a {@link TargetService} bean
	 *
	 * @param targets a collection of targets
	 * @return the bean
	 */
	@Bean
	public TargetService targetService(List<TargetFactory<?>> targets) {
		return new TargetService(targets);
	}

	/**
	 * Provide a {@link BackingServicesProvisionService} bean
	 *
	 * @param deployerClient the DeployerClient bean
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean
	public BackingServicesProvisionService backingServicesProvisionService(DeployerClient deployerClient) {
		return new DefaultBackingServicesProvisionService(deployerClient);
	}

	/**
	 * Provide a {@link CreateServiceInstanceWorkflow} bean
	 *
	 * @param brokeredServices the BrokeredServices bean
	 * @param backingAppDeploymentService the BackingAppDeploymentService bean
	 * @param appsParametersTransformationService the BackingApplicationsParametersTransformationService bean
	 * @param servicesParametersTransformationService the BackingServicesParametersTransformationService bean
	 * @param credentialProviderService the CredentialProviderService bean
	 * @param targetService the TargetService bean
	 * @param backingServicesProvisionService the BackingServicesProvisionService bean
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceWorkflow appDeploymentCreateServiceInstanceWorkflow(
		BrokeredServices brokeredServices, BackingAppDeploymentService backingAppDeploymentService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		CredentialProviderService credentialProviderService, TargetService targetService,
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

	/**
	 * Provide a {@link UpdateServiceInstanceWorkflow} bean
	 *
	 * @param brokeredServices the BrokeredServices bean
	 * @param backingAppDeploymentService the BackingAppDeploymentService bean
	 * @param backingAppManagementService the BackingAppManagementService bean
	 * @param backingServicesProvisionService the BackingServicesProvisionService bean
	 * @param appsParametersTransformationService the BackingApplicationsParametersTransformationService bean
	 * @param servicesParametersTransformationService the BackingServicesParametersTransformationService bean
	 * @param targetService the TargetService bean
	 * @return the bean
	 */
	@Bean
	public UpdateServiceInstanceWorkflow appDeploymentUpdateServiceInstanceWorkflow(
		BrokeredServices brokeredServices,
		BackingAppDeploymentService backingAppDeploymentService,
		BackingAppManagementService backingAppManagementService,
		BackingServicesProvisionService backingServicesProvisionService,
		BackingApplicationsParametersTransformationService appsParametersTransformationService,
		BackingServicesParametersTransformationService servicesParametersTransformationService,
		TargetService targetService) {

		return new AppDeploymentUpdateServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			backingAppManagementService,
			backingServicesProvisionService,
			appsParametersTransformationService,
			servicesParametersTransformationService,
			targetService);
	}

	/**
	 * Provide a {@link DeleteServiceInstanceWorkflow} bean
	 *
	 * @param brokeredServices the BrokeredServices bean
	 * @param backingAppDeploymentService the BackingAppDeploymentService bean
	 * @param backingAppManagementService the BackingAppManagementService bean
	 * @param backingServicesProvisionService the BackingServicesProvisionService bean
	 * @param credentialProviderService the CredentialProviderService bean
	 * @param targetService the TargetService bean
	 * @return the bean
	 */
	@Bean
	public DeleteServiceInstanceWorkflow appDeploymentDeleteServiceInstanceWorkflow(
		BrokeredServices brokeredServices, BackingAppDeploymentService backingAppDeploymentService,
		BackingAppManagementService backingAppManagementService,
		BackingServicesProvisionService backingServicesProvisionService,
		CredentialProviderService credentialProviderService, TargetService targetService) {

		return new AppDeploymentDeleteServiceInstanceWorkflow(
			brokeredServices,
			backingAppDeploymentService,
			backingServicesProvisionService,
			credentialProviderService,
			targetService
		);
	}

	/**
	 * Provide a {@link WorkflowServiceInstanceService} bean
	 *
	 * @param stateRepository the ServiceInstanceStateRepository bean
	 * @param createWorkflows a collection of create workflows
	 * @param deleteWorkflows a collection of delete workflows
	 * @param updateWorkflows a collection of update workflows
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean(ServiceInstanceService.class)
	public WorkflowServiceInstanceService serviceInstanceService(ServiceInstanceStateRepository stateRepository,
		List<CreateServiceInstanceWorkflow> createWorkflows, List<DeleteServiceInstanceWorkflow> deleteWorkflows,
		List<UpdateServiceInstanceWorkflow> updateWorkflows) {
		return new WorkflowServiceInstanceService(stateRepository, createWorkflows, deleteWorkflows, updateWorkflows);
	}

	/**
	 * Provide a {@link WorkflowServiceInstanceBindingService} bean
	 *
	 * @param stateRepository the ServiceInstanceBindingStateRepository bean
	 * @param createServiceInstanceAppBindingWorkflows a collection of create app binding workflows
	 * @param createServiceInstanceRouteBindingWorkflows a collection of create route binding workflows
	 * @param deleteServiceInstanceBindingWorkflows a collection of update workflows
	 * @return the bean
	 */
	@Bean
	@ConditionalOnMissingBean(ServiceInstanceBindingService.class)
	public WorkflowServiceInstanceBindingService serviceInstanceBindingService(
		ServiceInstanceBindingStateRepository stateRepository,
		@Autowired(required = false) List<CreateServiceInstanceAppBindingWorkflow> createServiceInstanceAppBindingWorkflows,
		@Autowired(required = false) List<CreateServiceInstanceRouteBindingWorkflow> createServiceInstanceRouteBindingWorkflows,
		@Autowired(required = false) List<DeleteServiceInstanceBindingWorkflow> deleteServiceInstanceBindingWorkflows) {
		return new WorkflowServiceInstanceBindingService(stateRepository,
			createServiceInstanceAppBindingWorkflows, createServiceInstanceRouteBindingWorkflows,
			deleteServiceInstanceBindingWorkflows);
	}

}
