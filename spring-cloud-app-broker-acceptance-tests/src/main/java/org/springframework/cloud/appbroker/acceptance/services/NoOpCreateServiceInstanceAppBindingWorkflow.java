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

package org.springframework.cloud.appbroker.acceptance.services;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;

/**
 * A binding workflow which systematically returned hard coded credentials. Useful in backing service broker, to
 * support test service keys that returned credentials to brokered service bindings.
 */
public class NoOpCreateServiceInstanceAppBindingWorkflow implements CreateServiceInstanceAppBindingWorkflow {

	public static final Map<String, Object> CREDENTIALS = Collections.singletonMap("noop-binding-key", "noop-binding-value");

	private static final Logger LOG = LoggerFactory.getLogger(NoOpCreateServiceInstanceAppBindingWorkflow.class);

	@Value("${spring.cloud.openservicebroker.catalog.services[1].id}")
	private String backingServiceId;

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceBindingRequest request) {
		boolean isAcceptingRequest = request.getServiceDefinitionId().equals(backingServiceId);
		if (LOG.isInfoEnabled()) {
			LOG.info("Got request to accept service binding request: {} and returning {}=({} equals {})", request,
				isAcceptingRequest, request.getServiceDefinitionId(), backingServiceId);
		}
		return Mono.just(isAcceptingRequest);
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder> buildResponse(
		CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceAppBindingResponse.CreateServiceInstanceAppBindingResponseBuilder responseBuilder) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Got request to create service binding: " + request);
		}
		responseBuilder.credentials(CREDENTIALS);
		return Mono.just(responseBuilder);
	}

}
