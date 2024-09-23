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

import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.deployer.DeployerClient;
import org.springframework.cloud.appbroker.deployer.cloudfoundry.CloudFoundryTargetProperties;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.EnvironmentMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.ParameterMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.parameters.PropertyMappingParametersTransformerFactory;
import org.springframework.cloud.appbroker.extensions.targets.ServiceInstanceGuidSuffix;
import org.springframework.cloud.appbroker.extensions.targets.SpacePerServiceInstance;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.manager.BackingAppManagementService;
import org.springframework.cloud.appbroker.manager.ManagementClient;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceRouteBindingWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceBindingService;
import org.springframework.cloud.appbroker.state.ServiceInstanceBindingStateRepository;
import org.springframework.cloud.appbroker.state.ServiceInstanceStateRepository;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentCreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentDeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.workflow.instance.AppDeploymentUpdateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppBrokerAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(
			CloudFoundryAppDeployerAutoConfiguration.class,
			AppBrokerAutoConfiguration.class
		));

	@Test
	void servicesAreCreatedWithCloudFoundryConfigured() {
		configuredContext()
			.run(context -> {
				assertBeansCreated(context);
				assertPropertiesLoaded(context);

				assertThat(context)
					.hasSingleBean(ServiceInstanceBindingService.class)
					.getBean(ServiceInstanceBindingService.class)
					.isExactlyInstanceOf(WorkflowServiceInstanceBindingService.class);
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

	@Test
	void bindingServiceIsNotCreatedIfProvided() {
		configuredContext()
			.withUserConfiguration(CustomBindingServiceConfiguration.class)
			.run(context -> {
				assertBeansCreated(context);

				assertThat(context)
					.hasSingleBean(ServiceInstanceBindingService.class)
					.getBean(ServiceInstanceBindingService.class)
					.isExactlyInstanceOf(TestServiceInstanceBindingService.class);
			});
	}

	@Test
	void serviceInstanceIsNotCreatedIfProvided() {
		configuredContext()
			.withUserConfiguration(CustomServiceConfiguration.class)
			.run(context -> {
				assertBeansCreated(context);

				assertThat(context)
					.hasSingleBean(ServiceInstanceService.class)
					.getBean(ServiceInstanceService.class)
					.isExactlyInstanceOf(TestServiceInstanceService.class);
			});
	}

	@Test
	void clientCredentialsNotAllowedWhenUsernameAndPasswordSet() {
		assertThatThrownBy(() -> this.contextRunner
			.withPropertyValues("spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret",
				"spring.cloud.appbroker.deployer.cloudfoundry.client_id=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.client_secret=secret")
			.run(Lifecycle::start));
	}

	@Test
	void clientIdWithoutSecretNotAllowed() {
		assertThatThrownBy(() -> this.contextRunner
			.withPropertyValues("spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.client_id=user")
			.run(Lifecycle::start));
	}

	@Test
	void configureCloudFoundryClientWithClientCredentials() {
		this.contextRunner
			.withPropertyValues("spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.client_id=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.client_secret=secret")
			.run(context -> {
				assertThat(context).hasSingleBean(TokenProvider.class);
				assertThat(context).hasSingleBean(ReactorCloudFoundryClient.class);
			});
	}

	@Test
	void serviceInstanceStateRepositoryIsNotCreatedIfProvided() {
		configuredContext()
			.withUserConfiguration(CustomStateRepositoriesConfiguration.class)
			.run(context -> {
				assertBeansCreated(context);

				assertThat(context)
					.hasSingleBean(ServiceInstanceStateRepository.class)
					.getBean(ServiceInstanceStateRepository.class)
					.isExactlyInstanceOf(TestServiceInstanceStateRepository.class);
			});
	}

	@Test
	void brokeredServicesIsNotCreatedIfProvided() {
		configuredContext()
			.withUserConfiguration(CustomBrokeredServicesConfiguration.class)
			.run(context -> {
				assertBeansCreated(context);

				assertThat(context)
					.hasSingleBean(BrokeredServices.class)
					.getBean(BrokeredServices.class)
					.isEqualTo(new CustomBrokeredServicesConfiguration().brokeredServices());
			});
	}

	@Test
	void serviceInstanceBindingStateRepositoryIsNotCreatedIfProvided() {
		configuredContext()
			.withUserConfiguration(CustomStateRepositoriesConfiguration.class)
			.run(context -> {
				assertBeansCreated(context);

				assertThat(context)
					.hasSingleBean(ServiceInstanceBindingStateRepository.class)
					.getBean(ServiceInstanceBindingStateRepository.class)
					.isExactlyInstanceOf(TestServiceInstanceBindingStateRepository.class);
			});
	}

	private ApplicationContextRunner configuredContext() {
		return this.contextRunner
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

				"spring.cloud.appbroker.deployer.cloudfoundry.api-host=https://api.example.local",
				"spring.cloud.appbroker.deployer.cloudfoundry.username=user",
				"spring.cloud.appbroker.deployer.cloudfoundry.password=secret",
				"spring.cloud.appbroker.deployer.cloudfoundry.staging-timeout=3",
				"spring.cloud.appbroker.deployer.cloudfoundry.deployment-timeout=4"
			);
	}

	private void assertBeansCreated(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(DeployerClient.class);
		assertThat(context).hasSingleBean(ManagementClient.class);
		assertThat(context).hasSingleBean(BrokeredServices.class);

		assertThat(context).hasSingleBean(ServiceInstanceStateRepository.class);
		assertThat(context).hasSingleBean(ServiceInstanceBindingStateRepository.class);

		assertThat(context).hasSingleBean(BackingAppDeploymentService.class);
		assertThat(context).hasSingleBean(BackingAppManagementService.class);
		assertThat(context).hasSingleBean(BackingServicesProvisionService.class);

		assertThat(context).hasSingleBean(BackingApplicationsParametersTransformationService.class);
		assertThat(context).hasSingleBean(EnvironmentMappingParametersTransformerFactory.class);
		assertThat(context).hasSingleBean(PropertyMappingParametersTransformerFactory.class);

		assertThat(context).hasSingleBean(BackingServicesParametersTransformationService.class);
		assertThat(context).hasSingleBean(ParameterMappingParametersTransformerFactory.class);

		assertThat(context).hasSingleBean(TargetService.class);

		assertThat(context).hasSingleBean(SpacePerServiceInstance.class);
		assertThat(context).hasSingleBean(ServiceInstanceGuidSuffix.class);

		assertThat(context).hasSingleBean(AppDeploymentCreateServiceInstanceWorkflow.class);
		assertThat(context).hasSingleBean(AppDeploymentDeleteServiceInstanceWorkflow.class);
		assertThat(context).hasSingleBean(AppDeploymentUpdateServiceInstanceWorkflow.class);

		assertThat(context).doesNotHaveBean(CreateServiceInstanceAppBindingWorkflow.class);
		assertThat(context).doesNotHaveBean(CreateServiceInstanceRouteBindingWorkflow.class);
		assertThat(context).doesNotHaveBean(DeleteServiceInstanceBindingWorkflow.class);
	}

	private void assertPropertiesLoaded(AssertableApplicationContext context) {
		CloudFoundryTargetProperties cloudFoundryTargetProperties = context.getBean(CloudFoundryTargetProperties.class);
		assertThat(cloudFoundryTargetProperties.getUsername()).isEqualTo("user");
		assertThat(cloudFoundryTargetProperties.getPassword()).isEqualTo("secret");
		assertThat(cloudFoundryTargetProperties.getDeploymentTimeout()).isEqualTo(4L);
		assertThat(cloudFoundryTargetProperties.getStagingTimeout()).isEqualTo(3L);

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
	}

	@Configuration
	public static class CustomBindingServiceConfiguration {

		@Bean
		public ServiceInstanceBindingService serviceInstanceBindingService() {
			return new TestServiceInstanceBindingService();
		}

	}

	@Configuration
	public static class CustomServiceConfiguration {

		@Bean
		public ServiceInstanceService serviceInstanceService() {
			return new TestServiceInstanceService();
		}

	}

	@Configuration
	public static class CustomBrokeredServicesConfiguration {

		@Bean
		public BrokeredServices brokeredServices() {
			return BrokeredServices.builder().service(
				BrokeredService.builder()
					.serviceName("single-service")
					.planName("service1-plan1")
					.build())
				.build();
		}
	}

	@Configuration
	public static class CustomStateRepositoriesConfiguration {

		@Bean
		public ServiceInstanceStateRepository serviceInstanceStateRepository() {
			return new TestServiceInstanceStateRepository();
		}

		@Bean
		public ServiceInstanceBindingStateRepository serviceInstanceBindingStateRepository() {
			return new TestServiceInstanceBindingStateRepository();
		}

	}

	private static class TestServiceInstanceBindingService implements ServiceInstanceBindingService {
	}

	private static class TestServiceInstanceService implements ServiceInstanceService {

		@Override
		public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
			return Mono.empty();
		}

		@Override
		public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
			return Mono.empty();
		}

	}

	private static class TestServiceInstanceStateRepository implements ServiceInstanceStateRepository {}

	private static class TestServiceInstanceBindingStateRepository implements ServiceInstanceBindingStateRepository {}

}
