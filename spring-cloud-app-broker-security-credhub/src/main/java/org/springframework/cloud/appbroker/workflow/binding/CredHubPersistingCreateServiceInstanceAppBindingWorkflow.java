/*
 * Copyright 2016-2018 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.core.annotation.Order;
import org.springframework.credhub.core.CredHubOperations;
import org.springframework.credhub.support.ServiceInstanceCredentialName;
import org.springframework.credhub.support.json.JsonCredentialRequest;
import org.springframework.util.CollectionUtils;

@Order(50)
public class CredHubPersistingCreateServiceInstanceAppBindingWorkflow implements CreateServiceInstanceAppBindingWorkflow {

	private static final Logger LOG = Loggers.getLogger(CredHubPersistingCreateServiceInstanceAppBindingWorkflow.class);

	private static final String CREDENTIALS_KEY = "credhub-ref";

	private static final String CREDENTIALS_NAME = "credentials-json";

	private final String appName;

	private final CredHubOperations credHubOperations;

	public CredHubPersistingCreateServiceInstanceAppBindingWorkflow(CredHubOperations credHubOperations, String appName) {
		this.credHubOperations = credHubOperations;
		this.appName = appName;
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponseBuilder> buildResponse(CreateServiceInstanceBindingRequest request,
																			  CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.build())
				   .flatMap(response -> {
					   if (!CollectionUtils.isEmpty(response.getCredentials())) {
						   return persistBindingCredentials(request, response)
							   .doOnRequest(l -> LOG.debug("Storing binding credentials in CredHub"))
							   .doOnSuccess(r -> LOG.debug("Finished storing binding credentials in CredHub"))
							   .doOnError(exception -> LOG.debug("Error storing binding credentials in CredHub with error {}", exception));
					   }
					   return Mono.just(responseBuilder);
				   });
	}

	private Mono<CreateServiceInstanceAppBindingResponseBuilder> persistBindingCredentials(CreateServiceInstanceBindingRequest request,
																						   CreateServiceInstanceAppBindingResponse response) {
		return Mono.just(ServiceInstanceCredentialName.builder()
			.serviceBrokerName(this.appName)
			.serviceOfferingName(request.getServiceDefinitionId())
			.serviceBindingId(request.getBindingId())
			.credentialName(CREDENTIALS_NAME)
			.build())
			.flatMap(credentialName -> Mono.fromCallable(() -> credHubOperations
				.credentials()
				.write(JsonCredentialRequest.builder()
					.name(credentialName)
					.value(response.getCredentials())
					.build()))
				.thenReturn(CreateServiceInstanceAppBindingResponse
					.builder()
					.async(response.isAsync())
					.bindingExisted(response.isBindingExisted())
					.credentials(CREDENTIALS_KEY, credentialName.getName())
					.operation(response.getOperation())
					.syslogDrainUrl(response.getSyslogDrainUrl())
					.volumeMounts(response.getVolumeMounts())));
	}

}
