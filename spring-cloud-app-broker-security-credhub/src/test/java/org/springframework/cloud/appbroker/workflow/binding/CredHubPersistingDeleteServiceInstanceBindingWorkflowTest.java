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

package org.springframework.cloud.appbroker.workflow.binding;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.credhub.core.ReactiveCredHubOperations;
import org.springframework.credhub.core.credential.ReactiveCredHubCredentialOperations;
import org.springframework.credhub.core.permission.ReactiveCredHubPermissionOperations;
import org.springframework.credhub.core.permissionV2.ReactiveCredHubPermissionV2Operations;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.CredentialPermission;
import org.springframework.credhub.support.CredentialSummary;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import org.springframework.credhub.support.permissions.Actor;
import org.springframework.credhub.support.permissions.Operation;
import org.springframework.credhub.support.permissions.Permission;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CredHubPersistingDeleteServiceInstanceBindingWorkflowTest {

	@Mock
	private ReactiveCredHubOperations credHubOperations;

	@Mock
	private ReactiveCredHubCredentialOperations credHubCredentialOperations;

	@Mock
	private ReactiveCredHubPermissionV2Operations credHubPermissionV2Operations;

	@Mock
	private ReactiveCredHubPermissionOperations credHubPermissionOperations;

	private CredHubPersistingDeleteServiceInstanceBindingWorkflow workflow;

	@BeforeEach
	void setUp() {
		this.workflow = new CredHubPersistingDeleteServiceInstanceBindingWorkflow(credHubOperations, "test-app-name");
	}

	@Test
	void deleteCredentialsFromCredHubWhenFound() {
		CredentialName credentialName = ServiceInstanceCredentialName.builder()
			.serviceBrokerName("test-app-name")
			.serviceOfferingName("foo-definition-id")
			.serviceBindingId("foo-binding-id")
			.credentialName("credentials-json")
			.build();

		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder =
			DeleteServiceInstanceBindingResponse.builder();

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubOperations.permissionsV2())
			.willReturn(credHubPermissionV2Operations);

		given(this.credHubOperations.permissions())
			.willReturn(credHubPermissionOperations);

		given(this.credHubCredentialOperations.findByName(credentialName))
			.willReturn(Flux.fromIterable(Collections.singletonList(new CredentialSummary(credentialName))));

		CredentialPermission credentialPermission = new CredentialPermission(credentialName,
			Permission.builder().app("app-id").operation(Operation.READ).build());
		ReflectionTestUtils.setField(credentialPermission, "uuid", "permission-uuid");

		given(this.credHubPermissionV2Operations.getPermissionsByPathAndActor(any(), any()))
			.willReturn(Mono.just(credentialPermission));

		given(this.credHubPermissionV2Operations.deletePermission("permission-uuid"))
			.willReturn(Mono.empty());

		Permission permission = Permission.builder().operation(Operation.READ).client("client-id").build();
		given(this.credHubPermissionOperations.getPermissions(any()))
			.willReturn(Flux.just(permission));

		given(this.credHubCredentialOperations.deleteByName(any()))
			.willReturn(Mono.empty());

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.expectNext(responseBuilder)
			.verifyComplete();

		verify(this.credHubCredentialOperations, times(1))
			.deleteByName(eq(credentialName));
		verify(this.credHubPermissionV2Operations, times(1))
			.getPermissionsByPathAndActor(eq(credentialName), eq(Actor.client("client-id")));
		verify(this.credHubPermissionV2Operations, times(1))
			.deletePermission(eq("permission-uuid"));

		verifyNoMoreInteractions(this.credHubCredentialOperations);
		verifyNoMoreInteractions(this.credHubPermissionOperations);
		verifyNoMoreInteractions(this.credHubPermissionV2Operations);
	}

	@Test
	void deleteCredentialsFromCredHubWhenNotFound() {
		DeleteServiceInstanceBindingRequest request = DeleteServiceInstanceBindingRequest
			.builder()
			.bindingId("foo-binding-id")
			.serviceInstanceId("foo-instance-id")
			.serviceDefinitionId("foo-definition-id")
			.build();

		DeleteServiceInstanceBindingResponseBuilder responseBuilder =
			DeleteServiceInstanceBindingResponse.builder();

		given(this.credHubOperations.credentials())
			.willReturn(credHubCredentialOperations);

		given(this.credHubCredentialOperations.findByName(any()))
			.willReturn(Flux.fromIterable(Collections.emptyList()));

		StepVerifier
			.create(this.workflow.buildResponse(request, responseBuilder))
			.expectNext(responseBuilder)
			.verifyComplete();

		verifyNoMoreInteractions(this.credHubCredentialOperations);
		verifyNoMoreInteractions(this.credHubPermissionOperations);
	}

}
