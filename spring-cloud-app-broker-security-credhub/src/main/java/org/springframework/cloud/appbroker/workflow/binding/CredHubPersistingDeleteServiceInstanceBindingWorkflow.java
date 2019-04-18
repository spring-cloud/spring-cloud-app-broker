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

package org.springframework.cloud.appbroker.workflow.binding;

import org.springframework.cloud.appbroker.service.DeleteServiceInstanceBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse.DeleteServiceInstanceBindingResponseBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.CredentialName;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

@Order(50)
public class CredHubPersistingDeleteServiceInstanceBindingWorkflow
	extends CredHubPersistingWorkflow
	implements DeleteServiceInstanceBindingWorkflow {

	private static final Logger LOG = Loggers.getLogger(CredHubPersistingDeleteServiceInstanceBindingWorkflow.class);

	private final CredHubOperations credHubOperations;

	public CredHubPersistingDeleteServiceInstanceBindingWorkflow(CredHubOperations credHubOperations, String appName) {
		super(appName);
		this.credHubOperations = credHubOperations;
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponseBuilder> buildResponse(DeleteServiceInstanceBindingRequest request,
																		   DeleteServiceInstanceBindingResponseBuilder responseBuilder) {
		return buildCredentialName(request.getServiceDefinitionId(), request.getBindingId())
			.filter(this::credentialExists)
			.flatMap(credentialName -> deleteBindingCredentials(credentialName)
					.doOnRequest(l -> LOG.debug("Deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
					.doOnSuccess(r -> LOG.debug("Finished deleting binding credentials with name '{}' in CredHub", credentialName.getName()))
					.doOnError(exception -> LOG.error("Error deleting binding credentials with name '{}' in CredHub with error: {}",
						credentialName.getName(), exception.getMessage())))
			.thenReturn(responseBuilder);
	}

	private boolean credentialExists(CredentialName credentialName) {
		return !credHubOperations.credentials().findByName(credentialName).isEmpty();
	}

	private Mono<Void> deleteBindingCredentials(CredentialName credentialName) {
		return Mono.fromCallable(() -> {
			credHubOperations.credentials().deleteByName(credentialName);
			return null;
		});
	}

}
