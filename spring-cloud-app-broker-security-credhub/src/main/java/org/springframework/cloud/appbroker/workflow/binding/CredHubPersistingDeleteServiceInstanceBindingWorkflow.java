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

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.credhub.core.ReactiveCredHubOperations;
import org.springframework.credhub.support.CredentialName;
import org.springframework.credhub.support.ServiceInstanceCredentialName;

@Order(50)
public class CredHubPersistingDeleteServiceInstanceBindingWorkflow extends CredHubPersistingWorkflow
	implements DeleteServiceInstanceBindingWorkflow {

	private static final Logger LOG = Loggers.getLogger(CredHubPersistingDeleteServiceInstanceBindingWorkflow.class);

	private final ReactiveCredHubOperations credHubOperations;

	public CredHubPersistingDeleteServiceInstanceBindingWorkflow(ReactiveCredHubOperations credHubOperations,
		String appName) {
		super(appName);
		this.credHubOperations = credHubOperations;
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponseBuilder> buildResponse(DeleteServiceInstanceBindingRequest request,
		DeleteServiceInstanceBindingResponseBuilder responseBuilder) {
		return buildCredentialName(request.getServiceDefinitionId(), request.getBindingId())
			.filterWhen(this::credentialExists)
			.delayUntil(this::deletePermission)
			.delayUntil(this::deleteCredential)
			.thenReturn(responseBuilder);
	}

	private Mono<Void> deleteCredential(ServiceInstanceCredentialName credentialName) {
		return credHubOperations.credentials()
			.deleteByName(credentialName)
			.doOnRequest(
				l -> LOG.debug("Deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
			.doOnSuccess(r -> LOG
				.debug("Finished deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
			.doOnError(exception -> LOG.error(
				String.format("Error deleting binding credentials with name '%s' in CredHub with error: '%s'",
					credentialName.getName(), exception.getMessage()), exception));
	}

	private Mono<Void> deletePermission(ServiceInstanceCredentialName credentialName) {
		return credHubOperations.permissions()
			.getPermissions(credentialName)
			.flatMap(permission -> credHubOperations.permissionsV2()
				.getPermissionsByPathAndActor(credentialName, permission.getActor()))
			.flatMap(credentialPermission -> credHubOperations.permissionsV2()
				.deletePermission(credentialPermission.getId())
				.doOnRequest(
					l -> LOG.debug("Deleting binding permission for credential with name '{}' in CredHub",
						credentialName.getName()))
				.doOnSuccess(r -> LOG
					.debug("Finished deleting binding permission for credential with name '{}' in CredHub",
						credentialName.getName()))
				.doOnError(exception -> LOG.error(
					String.format(
						"Error deleting binding permission for credential with name '%s' in CredHub with error: '%s'",
						credentialName.getName(), exception.getMessage()), exception)))
			.then();
	}

	private Mono<Boolean> credentialExists(CredentialName credentialName) {
		return credHubOperations.credentials()
			.findByName(credentialName)
			.collectList()
			.map(credentialSummaries -> !credentialSummaries.isEmpty());
	}

}
