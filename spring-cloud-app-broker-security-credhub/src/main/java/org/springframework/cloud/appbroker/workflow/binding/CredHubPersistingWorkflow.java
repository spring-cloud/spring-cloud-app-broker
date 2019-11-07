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

import reactor.core.publisher.Mono;

import org.springframework.credhub.support.ServiceInstanceCredentialName;

public class CredHubPersistingWorkflow {

	private static final String CREDENTIALS_NAME = "credentials-json";

	private final String appName;

	protected CredHubPersistingWorkflow(String appName) {
		this.appName = appName;
	}

	protected Mono<ServiceInstanceCredentialName> buildCredentialName(String serviceDefinitionId, String bindingId) {
		return Mono.just(ServiceInstanceCredentialName.builder()
			.serviceBrokerName(this.appName)
			.serviceOfferingName(serviceDefinitionId)
			.serviceBindingId(bindingId)
			.credentialName(CREDENTIALS_NAME)
			.build());
	}

}
