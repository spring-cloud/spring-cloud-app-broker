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

package org.springframework.cloud.appbroker.integration.fixtures;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@TestComponent
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name="spring.credhub.url")
public class TestBindingCredentialsProviderFixture implements CreateServiceInstanceAppBindingWorkflow {

	private final Map<String, Object> credentials;

	public TestBindingCredentialsProviderFixture() {
		this.credentials = new HashMap<>();
		this.credentials.put("credential1", "value1");
		this.credentials.put("credential2", "value2");
	}

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceBindingRequest request) {
		return Mono.just(true);
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponseBuilder> buildResponse(
		CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.credentials(this.credentials));
	}

	public Map<String, Object> getCredentials() {
		return credentials;
	}

}
