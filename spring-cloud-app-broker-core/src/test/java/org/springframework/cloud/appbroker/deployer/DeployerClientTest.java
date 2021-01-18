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

package org.springframework.cloud.appbroker.deployer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("UnassignedFluxMonoInstance")
class DeployerClientTest {

	private static final String APP_NAME = "helloworld";

	private static final String APP_ARCHIVE = "app.jar";

	private static final String APP_PATH = "classpath:/jars/" + APP_ARCHIVE;

	private static final String SERVICE_INSTANCE_NAME = "helloservice";

	private DeployerClient deployerClient;

	@Mock
	private AppDeployer appDeployer;

	private static ArgumentMatcher<DeployApplicationRequest> matchesDeploymentRequest(String appName, String appArchive,
		Map<String, String> properties,
		Map<String, Object> environment,
		List<String> services) {
		return request ->
			request.getName().equals(appName) &&
				request.getPath().equals(appArchive) &&
				request.getProperties().equals(properties) &&
				request.getEnvironment().equals(environment) &&
				request.getServices().equals(services);
	}

	private static ArgumentMatcher<UpdateApplicationRequest> matchesUpdateRequest(String appName, String appArchive,
		Map<String, String> properties,
		Map<String, Object> environment,
		List<String> services,
		String serviceInstanceId) {
		return request ->
			request.getName().equals(appName) &&
				request.getPath().equals(appArchive) &&
				request.getProperties().equals(properties) &&
				request.getEnvironment().equals(environment) &&
				request.getServices().equals(services) &&
				request.getServiceInstanceId().equals(serviceInstanceId);
	}

	@BeforeEach
	void setUp() {
		deployerClient = new DeployerClient(appDeployer);
	}

	@Nested
	class Deploy {

		@Test
		void shouldDeployApp() {
			// given
			setupAppDeployer();

			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

			// when
			StepVerifier.create(deployerClient.deploy(application, "instance-id"))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should()
				.deploy(argThat(matchesDeploymentRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
					Collections.emptyMap(), Collections.emptyList())));
		}

		@Test
		@SuppressWarnings("serial")
		void shouldDeployAppWithProperties() {
			setupAppDeployer();

			Map<String, String> properties = new HashMap<String, String>() {{
				put("memory", "1G");
				put("instances", "2");
			}};
			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.properties(properties)
				.build();

			// when
			StepVerifier.create(deployerClient.deploy(application, "instance-id"))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should().deploy(argThat(matchesDeploymentRequest(APP_NAME, APP_PATH, properties,
				Collections.emptyMap(), Collections.emptyList())));
		}

		@Test
		@SuppressWarnings("serial")
		void shouldDeployAppWithService() {
			setupAppDeployer();

			BackingApplication application =
				BackingApplication
					.builder()
					.name(APP_NAME)
					.path(APP_PATH)
					.services(ServicesSpec.builder()
						.serviceInstanceName("my-db-service")
						.build())
					.build();

			// when
			StepVerifier.create(deployerClient.deploy(application, "instance-id"))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should()
				.deploy(argThat(matchesDeploymentRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
					Collections.emptyMap(), Collections.singletonList("my-db-service"))));
		}

		@Test
		@SuppressWarnings("serial")
		void shouldDeployAppWithEnvironmentVariables() {
			setupAppDeployer();

			Map<String, Object> environment = new HashMap<String, Object>() {{
				put("ENV_VAR_1", "value1");
				put("ENV_VAR_2", "value2");
			}};
			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.environment(environment)
				.build();

			// when
			StepVerifier.create(deployerClient.deploy(application, "instance-id"))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should()
				.deploy(argThat(matchesDeploymentRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
					environment, Collections.emptyList())));
		}

		private void setupAppDeployer() {
			given(appDeployer.deploy(any()))
				.willReturn(Mono.just(DeployApplicationResponse.builder()
					.name(APP_NAME)
					.build()));
		}
	}

	@Nested
	class PreUpdate {

		@Test
		void shouldPrepareAppForUpdate() {
			// given
			setupAppDeployer();
			final String serviceInstanceId = "instance-id";

			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

			// when
			StepVerifier.create(deployerClient.preUpdate(application, serviceInstanceId))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should().preUpdate(argThat(matchesUpdateRequest(APP_NAME,
				APP_PATH, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), serviceInstanceId)));
		}


		private void setupAppDeployer() {
			given(appDeployer.preUpdate(any()))
				.willReturn(Mono.just(UpdateApplicationResponse.builder()
					.name(APP_NAME)
					.build()));
		}
	}

	@Nested
	class Update {

		@Test
		void shouldUpdateApp() {
			// given
			setupAppDeployer();
			final String serviceInstanceId = "instance-id";

			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

			// when
			StepVerifier.create(deployerClient.update(application, serviceInstanceId))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should().update(argThat(matchesUpdateRequest(APP_NAME,
				APP_PATH, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), serviceInstanceId)));
		}


		private void setupAppDeployer() {
			given(appDeployer.update(any()))
				.willReturn(Mono.just(UpdateApplicationResponse.builder()
					.name(APP_NAME)
					.build()));
		}
	}

	@Nested
	class Undeploy {

		@Test
		void shouldUndeployApp() {
			given(appDeployer.undeploy(any()))
				.willReturn(Mono.just(UndeployApplicationResponse.builder()
					.name(APP_NAME)
					.build()));

			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

			// when
			StepVerifier.create(deployerClient.undeploy(application))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should().undeploy(argThat(request -> APP_NAME.equals(request.getName())));
		}

		@Test
		void shouldNotReturnErrorWhenUndeployingAppThatDoesNotExist() {
			given(appDeployer.undeploy(any()))
				.willReturn(Mono.error(new IllegalStateException("app does not exist")));

			BackingApplication application = BackingApplication.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

			// when
			StepVerifier.create(deployerClient.undeploy(application))
				// then
				.expectNext(APP_NAME)
				.verifyComplete();

			then(appDeployer).should().undeploy(argThat(request -> APP_NAME.equals(request.getName())));
		}

	}

	@Nested
	class CreateServiceInstance {

		@Test
		void shouldCreateServiceInstance() {
			given(appDeployer.createServiceInstance(any()))
				.willReturn(Mono.just(CreateServiceInstanceResponse.builder()
					.name(SERVICE_INSTANCE_NAME)
					.build()));

			BackingService service = BackingService.builder()
				.serviceInstanceName(SERVICE_INSTANCE_NAME)
				.build();

			// when
			StepVerifier.create(deployerClient.createServiceInstance(service))
				// then
				.expectNext(SERVICE_INSTANCE_NAME)
				.verifyComplete();

			then(appDeployer).should().createServiceInstance(argThat(request ->
				SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName())));
		}

	}

	@Nested
	class UpdateServiceInstance {

		@Test
		void shouldUpdateServiceInstance() {
			given(appDeployer.updateServiceInstance(any()))
				.willReturn(Mono.just(UpdateServiceInstanceResponse.builder()
					.name(SERVICE_INSTANCE_NAME)
					.build()));

			BackingService service = BackingService.builder()
				.serviceInstanceName(SERVICE_INSTANCE_NAME)
				.build();

			// when
			StepVerifier.create(deployerClient.updateServiceInstance(service))
				// then
				.expectNext(SERVICE_INSTANCE_NAME)
				.verifyComplete();

			then(appDeployer).should().updateServiceInstance(argThat(request ->
				SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName())));
		}

		@Test
		void shouldReturnErrorWhenUpdatingServiceInstanceThatDoesNotExist() {
			given(appDeployer.updateServiceInstance(any()))
				.willReturn(Mono.error(new IllegalStateException("service instance does not exist")));

			BackingService service = BackingService.builder()
				.serviceInstanceName(SERVICE_INSTANCE_NAME)
				.build();

			// when
			StepVerifier.create(deployerClient.updateServiceInstance(service))
				// then
				.expectErrorMessage("service instance does not exist")
				.verify();

			then(appDeployer).should().updateServiceInstance(argThat(request ->
				SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName())));
		}

	}

	@Nested
	class DeleteServiceInstance {

		@Test
		void shouldDeleteServiceInstance() {
			given(appDeployer.deleteServiceInstance(any()))
				.willReturn(Mono.just(DeleteServiceInstanceResponse.builder()
					.name(SERVICE_INSTANCE_NAME)
					.build()));

			BackingService service = BackingService.builder()
				.serviceInstanceName(SERVICE_INSTANCE_NAME)
				.build();

			// when
			StepVerifier.create(deployerClient.deleteServiceInstance(service))
				// then
				.expectNext(SERVICE_INSTANCE_NAME)
				.verifyComplete();

			then(appDeployer).should().deleteServiceInstance(argThat(request ->
				SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName())));
		}

		@Test
		void shouldNotReturnErrorWhenDeletingServiceInstanceThatDoesNotExist() {
			given(appDeployer.deleteServiceInstance(any()))
				.willReturn(Mono.error(new IllegalStateException("service instance does not exist")));

			BackingService service = BackingService.builder()
				.serviceInstanceName(SERVICE_INSTANCE_NAME)
				.build();

			// when
			StepVerifier.create(deployerClient.deleteServiceInstance(service))
				// then
				.expectNext(SERVICE_INSTANCE_NAME)
				.verifyComplete();

			then(appDeployer).should().deleteServiceInstance(argThat(request ->
				SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName())));
		}

	}

}
