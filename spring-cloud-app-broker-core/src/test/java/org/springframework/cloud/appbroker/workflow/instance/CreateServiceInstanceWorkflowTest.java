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

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import reactor.core.publisher.Mono;


import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateServiceInstanceWorkflowTest {

	@Mock
	private BackingAppDeploymentService backingAppDeploymentService;

	@Test
	void shouldCreateServiceInstance() {
		given(this.backingAppDeploymentService.deploy(any(BackingApplications.class)))
			.willReturn(Mono.just("deployment-id-app1"));

		// given that properties contains app details
		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(createBackingApplications(), backingAppDeploymentService);

		// when
		createServiceInstanceWorkflow.create(Collections.emptyMap());

		// then deployer should be called with the application details
		ArgumentCaptor<BackingApplications> argumentCaptor = ArgumentCaptor.forClass(BackingApplications.class);
		verify(backingAppDeploymentService).deploy(argumentCaptor.capture());

		BackingApplication backingApplication = argumentCaptor.getValue().get(0);
		assertThat(backingApplication.getName()).isEqualTo("helloworldapp");
		assertThat(backingApplication.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication.getEnvironment()).isEqualTo(Collections.emptyMap());
	}

	@Test
	void shouldCreateServiceInstanceWithEnvironment() {
		given(this.backingAppDeploymentService.deploy(any(BackingApplications.class)))
			.willReturn(Mono.just("deployment-id-app1"));

		// given that properties contains app details and environment variables
		Map<String, String> environment = singletonMap("ENV_VAR_1", "value from environment");
		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(createBackingApplicationWithEnvironment(environment), backingAppDeploymentService);

		// when
		createServiceInstanceWorkflow.create(Collections.emptyMap());

		// then deployer should be called with the application details and the environment variables
		ArgumentCaptor<BackingApplications> argumentCaptor = ArgumentCaptor.forClass(BackingApplications.class);
		verify(backingAppDeploymentService).deploy(argumentCaptor.capture());

		BackingApplication backingApplication = argumentCaptor.getValue().get(0);
		assertThat(backingApplication.getName()).isEqualTo("helloworldapp");
		assertThat(backingApplication.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication.getEnvironment()).isEqualTo(singletonMap("ENV_VAR_1", "value from environment"));
	}

	@Test
	void shouldCreateServiceInstanceWithParameters() {
		given(this.backingAppDeploymentService.deploy(any(BackingApplications.class)))
			.willReturn(Mono.just("deployment-id-app1"));

		// given that properties contains app details
		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(createBackingApplications(), backingAppDeploymentService);

		// when create is called with parameters
		Map<String, Object> parameters = singletonMap("ENV_VAR_1", "value from parameters");
		createServiceInstanceWorkflow.create(parameters);

		// then deployer should be called with the application details and environment variables
		ArgumentCaptor<BackingApplications> argumentCaptor = ArgumentCaptor.forClass(BackingApplications.class);
		verify(backingAppDeploymentService).deploy(argumentCaptor.capture());

		BackingApplication backingApplication = argumentCaptor.getValue().get(0);
		assertThat(backingApplication.getName()).isEqualTo("helloworldapp");
		assertThat(backingApplication.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication.getEnvironment()).isEqualTo(singletonMap("ENV_VAR_1", "value from parameters"));
	}

	@Test
	void shouldCreateServiceInstanceOverwritingEnvironmentPropertiesWithParameters() {
		given(this.backingAppDeploymentService.deploy(any(BackingApplications.class)))
			.willReturn(Mono.just("deployment-id-app1"));

		// given that properties contains app details and environment variables
		Map<String, String> environment = singletonMap("ENV_VAR_1", "value from environment");
		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(createBackingApplicationWithEnvironment(environment), backingAppDeploymentService);

		// when create is called with parameters
		Map<String, Object> parameters = singletonMap("ENV_VAR_1", "value from parameters");
		createServiceInstanceWorkflow.create(parameters);

		// then deployer should contain the environment with the value from the parameters
		ArgumentCaptor<BackingApplications> argumentCaptor = ArgumentCaptor.forClass(BackingApplications.class);
		verify(backingAppDeploymentService).deploy(argumentCaptor.capture());

		BackingApplication backingApplication = argumentCaptor.getValue().get(0);
		assertThat(backingApplication.getName()).isEqualTo("helloworldapp");
		assertThat(backingApplication.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication.getEnvironment()).isEqualTo(singletonMap("ENV_VAR_1", "value from parameters"));
	}

	@Test
	void shouldCreateServiceInstanceWithMultipleApplicationsAndProperties() {
		given(this.backingAppDeploymentService.deploy(any(BackingApplications.class)))
			.willReturn(Mono.just("deployment-id-app1"));

		// given that properties contains two apps
		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("helloworldapp1")
				.path("http://myfiles/app.jar")
				.build())
			.backingApplication(BackingApplication.builder()
				.name("helloworldapp2")
				.path("http://myfiles/app.jar")
				.build())
			.build();

		CreateServiceInstanceWorkflow createServiceInstanceWorkflow =
			new CreateServiceInstanceWorkflow(backingApplications, backingAppDeploymentService);

		// when create is called with parameters
		Map<String, Object> parameters = singletonMap("ENV_VAR_1", "value from parameters");
		createServiceInstanceWorkflow.create(parameters);

		// then deployer should contain the environment with the value from the parameters
		ArgumentCaptor<BackingApplications> argumentCaptor = ArgumentCaptor.forClass(BackingApplications.class);
		verify(backingAppDeploymentService).deploy(argumentCaptor.capture());

		BackingApplication backingApplication1 = argumentCaptor.getValue().get(0);
		assertThat(backingApplication1.getName()).isEqualTo("helloworldapp1");
		assertThat(backingApplication1.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication1.getEnvironment()).isEqualTo(singletonMap("ENV_VAR_1", "value from parameters"));

		BackingApplication backingApplication2 = argumentCaptor.getValue().get(1);
		assertThat(backingApplication2.getName()).isEqualTo("helloworldapp2");
		assertThat(backingApplication2.getPath()).isEqualTo("http://myfiles/app.jar");
		assertThat(backingApplication2.getEnvironment()).isEqualTo(singletonMap("ENV_VAR_1", "value from parameters"));
	}

	private BackingApplications createBackingApplications() {
		return createBackingApplicationWithEnvironment(null);
	}

	private BackingApplications createBackingApplicationWithEnvironment(Map<String, String> parameters) {
		BackingApplication.BackingApplicationBuilder backingApplicationBuilder = BackingApplication.builder()
			.name("helloworldapp")
			.path("http://myfiles/app.jar");
		if (parameters != null) {
			backingApplicationBuilder.environment(parameters);
		}
		return BackingApplications.builder()
			.backingApplication(backingApplicationBuilder.build())
			.build();
	}
}