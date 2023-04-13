/*
 * Copyright 2002-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.workflow.binding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.VolumeMount;
import org.springframework.credhub.core.ReactiveCredHubOperations;
import org.springframework.credhub.core.credential.ReactiveCredHubCredentialOperations;
import org.springframework.credhub.core.permissionV2.ReactiveCredHubPermissionV2Operations;
import org.springframework.credhub.support.CredentialDetails;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.CredentialPermission;
import org.springframework.credhub.support.ServiceInstanceCredentialName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CredHubPersistingCreateServiceInstanceAppBindingWorkflowTest {

	@Mock
	private ReactiveCredHubOperations credHubOperations;

	@Mock
	private ReactiveCredHubCredentialOperations credHubCredentialOperations;

	@Mock
	private ReactiveCredHubPermissionV2Operations credHubPermissionOperations;

	private CredHubPersistingCreateServiceInstanceAppBindingWorkflow workflow;

	@BeforeEach
	void setUp() {
		this.workflow = new CredHubPersistingCreateServiceInstanceAppBindingWorkflow(credHubOperations,
			"test-app-name");
	}

	@Test
	void noBindingCredentials() {
		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.build();

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
			.builder()
			.bindingExisted(true)
			.syslogDrainUrl("https://logs.example.local")
			.volumeMounts(VolumeMount.builder().build())
			.volumeMounts(VolumeMount.builder().build())
			.volumeMounts(Arrays.asList(
				VolumeMount.builder().build(),
				VolumeMount.builder().build()
			));

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.assertNext(createServiceInstanceAppBindingResponseBuilder -> {
				CreateServiceInstanceAppBindingResponse response = createServiceInstanceAppBindingResponseBuilder
					.build();
				assertThat(response.isBindingExisted()).isEqualTo(true);
				assertThat(response.getCredentials()).hasSize(0);
				assertThat(response.getSyslogDrainUrl()).isEqualTo("https://logs.example.local");
				assertThat(response.getVolumeMounts()).hasSize(4);

			})
			.verifyComplete();

		verifyNoInteractions(this.credHubCredentialOperations);
	}

	@Test
	@SuppressWarnings("serial")
	void storeCredentialsInCredHub() {
		CredentialName credentialName = ServiceInstanceCredentialName.builder()
			.serviceBrokerName("test-app-name")
			.serviceOfferingName("foo-definition-id")
			.serviceBindingId("foo-binding-id")
			.credentialName("credentials-json")
			.build();

		CreateServiceInstanceBindingRequest request = CreateServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.bindResource(BindResource.builder()
				.appGuid("app-id")
				.properties("credential_client_id", "client-id")
				.build())
			.build();

		Map<String, Object> credentials = new HashMap<String, Object>() {{
			put("credential4", "value4");
			put("credential5", "value5");
		}};

		CreateServiceInstanceAppBindingResponseBuilder responseBuilder = CreateServiceInstanceAppBindingResponse
			.builder()
			.bindingExisted(true)
			.credentials("credential1", "value1")
			.credentials("credential2", 2)
			.credentials("credential3", true)
			.credentials(credentials)
			.syslogDrainUrl("https://logs.example.local")
			.volumeMounts(VolumeMount.builder().build())
			.volumeMounts(VolumeMount.builder().build())
			.volumeMounts(Arrays.asList(
				VolumeMount.builder().build(),
				VolumeMount.builder().build()
			));

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);
		given(this.credHubOperations.permissionsV2())
			.willReturn(credHubPermissionOperations);

		given(this.credHubCredentialOperations.write(any()))
			.willReturn(Mono.just(new CredentialDetails<>()));

		given(this.credHubPermissionOperations.addPermissions(any(), any()))
			.willReturn(Mono.just(Mockito.mock(CredentialPermission.class)));

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.assertNext(createServiceInstanceAppBindingResponseBuilder -> {
				CreateServiceInstanceAppBindingResponse response = createServiceInstanceAppBindingResponseBuilder
					.build();
				assertThat(response.isBindingExisted()).isEqualTo(true);
				assertThat(response.getCredentials()).hasSize(1);
				assertThat(response.getCredentials().get("credhub-ref")).isEqualTo(credentialName.getName());
				assertThat(response.getSyslogDrainUrl()).isEqualTo("https://logs.example.local");
				assertThat(response.getVolumeMounts()).hasSize(4);

			})
			.verifyComplete();

		verify(this.credHubCredentialOperations, times(1))
			.write(any());
		verify(this.credHubPermissionOperations, times(1))
			.addPermissions(any(), any());
		verifyNoMoreInteractions(this.credHubCredentialOperations);
	}

}
