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

package org.springframework.cloud.appbroker.deployer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
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

	private static final String SERVICE_INSTANCE_PLAN_NAME = "helloplan";

	private DeployerClient deployerClient;

	@Mock
	private AppDeployer appDeployer;

	@BeforeEach
	void setUp() {
		deployerClient = new DeployerClient(appDeployer);
	}

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

		then(appDeployer).should().deploy(argThat(matchesRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
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

		then(appDeployer).should().deploy(argThat(matchesRequest(APP_NAME, APP_PATH, properties,
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

		then(appDeployer).should().deploy(argThat(matchesRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
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

		then(appDeployer).should().deploy(argThat(matchesRequest(APP_NAME, APP_PATH, Collections.emptyMap(),
			environment, Collections.emptyList())));
	}

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

	@Test
	void shouldUpdateServiceInstanceOnParamsUpdate() {
		given(appDeployer.updateServiceInstance(any()))
			.willReturn(Mono.just(UpdateServiceInstanceResponse.builder()
				.name(SERVICE_INSTANCE_NAME)
				.build()));

		BackingService service = BackingService.builder()
			.serviceInstanceName(SERVICE_INSTANCE_NAME)
			.plan(SERVICE_INSTANCE_PLAN_NAME)
			.parameters(Maps.newHashMap("some-updated-params-triggering-the-update", "true"))
			.build();

		// when
		StepVerifier.create(deployerClient.updateServiceInstance(service))
			// then
			.expectNext(SERVICE_INSTANCE_NAME)
			.verifyComplete();

		then(appDeployer).should()
			.updateServiceInstance(argThat(request -> SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName()) &&
				request.getPlan() == null));
	}

	@Test
	void shouldUpdateServiceInstanceOnPlanUpdate() {
		// given
		given(appDeployer.updateServiceInstance(any()))
			.willReturn(Mono.just(UpdateServiceInstanceResponse.builder()
				.name(SERVICE_INSTANCE_NAME)
				.build()));

		BackingService service = BackingService.builder()
			.serviceInstanceName(SERVICE_INSTANCE_NAME)
			.plan(SERVICE_INSTANCE_PLAN_NAME)
			.previousPlan("a-previous-plan")
			.build();

		// when
		StepVerifier.create(deployerClient.updateServiceInstance(service))
			// then
			.expectNext(SERVICE_INSTANCE_NAME)
			.verifyComplete();

		then(appDeployer).should().updateServiceInstance(
			argThat(request -> SERVICE_INSTANCE_NAME.equals(request.getServiceInstanceName()) &&
				SERVICE_INSTANCE_PLAN_NAME.equals(request.getPlan()))
		);
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

	private ArgumentMatcher<DeployApplicationRequest> matchesRequest(String appName, String appArchive,
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

	private void setupAppDeployer() {
		given(appDeployer.deploy(any()))
			.willReturn(Mono.just(DeployApplicationResponse.builder()
				.name(APP_NAME)
				.build()));
	}

}
