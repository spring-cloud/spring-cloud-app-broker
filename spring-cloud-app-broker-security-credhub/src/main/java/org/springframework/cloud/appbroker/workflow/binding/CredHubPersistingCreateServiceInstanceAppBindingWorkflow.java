/*
 * Copyright 2002-2019 the original author or authors
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

import org.springframework.credhub.support.permissions.Operation;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.core.annotation.Order;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.permissions.Permission;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.util.CollectionUtils;

@Order(50)
public class CredHubPersistingCreateServiceInstanceAppBindingWorkflow
	extends CredHubPersistingWorkflow
	implements CreateServiceInstanceAppBindingWorkflow {

	private static final Logger LOG = Loggers.getLogger(CredHubPersistingCreateServiceInstanceAppBindingWorkflow.class);

	private static final String CREDHUB_REF_KEY = "credhub-ref";

	private static final String CREDENTIAL_CLIENT_ID = "credential_client_id";

	private final CredHubOperations credHubOperations;

	public CredHubPersistingCreateServiceInstanceAppBindingWorkflow(CredHubOperations credHubOperations, String appName) {
		super(appName);
		this.credHubOperations = credHubOperations;
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponseBuilder> buildResponse(CreateServiceInstanceBindingRequest request,
																			  CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.build())
			.flatMap(response -> {
				if (!CollectionUtils.isEmpty(response.getCredentials())) {
					return buildCredentialName(request.getServiceDefinitionId(), request.getBindingId())
						.flatMap(credentialName -> persistBindingCredentials(request, response, credentialName)
							.doOnRequest(l -> LOG.debug("Storing binding credentials with name '{}' in CredHub", credentialName.getName()))
							.doOnSuccess(r -> LOG.debug("Finished storing binding credentials with name '{}' in CredHub", credentialName.getName()))
							.doOnError(exception -> LOG.error("Error storing binding credentials with name '{}' in CredHub with error: {}",
								credentialName.getName(), exception.getMessage())));
				}
				return Mono.just(responseBuilder);
			});
	}

	private Mono<CreateServiceInstanceAppBindingResponseBuilder> persistBindingCredentials(CreateServiceInstanceBindingRequest request,
																						   CreateServiceInstanceAppBindingResponse response,
																						   CredentialName credentialName) {
		return writeCredential(response, credentialName)
			.then(writePermissions(request, credentialName))
			.thenReturn(buildReplacementBindingResponse(response, credentialName));
	}

	private Mono<Void> writeCredential(CreateServiceInstanceAppBindingResponse response,
									   CredentialName credentialName) {
		return Mono.fromCallable(() -> {
			credHubOperations.credentials()
				.write(JsonCredentialRequest.builder()
					.name(credentialName)
					.value(response.getCredentials())
					.build());
			return null;
		});
	}

	private Mono<Void> writePermissions(CreateServiceInstanceBindingRequest request,
										CredentialName credentialName) {
		return Mono.fromCallable(() -> {
			BindResource bindResource = request.getBindResource();

			if (bindResource.getAppGuid() != null) {
				Permission permission = Permission.builder()
					.app(bindResource.getAppGuid())
					.operation(Operation.READ)
					.build();
				credHubOperations.permissionsV2().addPermissions(credentialName, permission);
			}

			if (bindResource.getProperty(CREDENTIAL_CLIENT_ID) != null) {
				Permission permission = Permission.builder()
					.client(bindResource.getProperty(CREDENTIAL_CLIENT_ID).toString())
					.operation(Operation.READ)
					.build();
				credHubOperations.permissionsV2().addPermissions(credentialName, permission);
			}

			return null;
		});
	}

	private CreateServiceInstanceAppBindingResponseBuilder buildReplacementBindingResponse(CreateServiceInstanceAppBindingResponse response,
																						   CredentialName credentialName) {
		return CreateServiceInstanceAppBindingResponse.builder()
			.async(response.isAsync())
			.bindingExisted(response.isBindingExisted())
			.credentials(CREDHUB_REF_KEY, credentialName.getName())
			.operation(response.getOperation())
			.syslogDrainUrl(response.getSyslogDrainUrl())
			.volumeMounts(response.getVolumeMounts());
	}

}
