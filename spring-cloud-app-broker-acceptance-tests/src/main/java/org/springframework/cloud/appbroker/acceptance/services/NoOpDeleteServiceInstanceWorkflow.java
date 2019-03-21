/*
 * Copyright 2016-2018 the original author or authors.
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse.DeleteServiceInstanceResponseBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NoOpDeleteServiceInstanceWorkflow implements DeleteServiceInstanceWorkflow {
	@Value("${spring.cloud.openservicebroker.catalog.services[1].id}")
	private String backingServiceId;

	@Override
	public Mono<Void> delete(DeleteServiceInstanceRequest request, DeleteServiceInstanceResponse response) {
		return Mono.empty();
	}

	@Override
	public Mono<Boolean> accept(DeleteServiceInstanceRequest request) {
		return Mono.just(request.getServiceDefinitionId().equals(backingServiceId));
	}

	@Override
	public Mono<DeleteServiceInstanceResponseBuilder> buildResponse(DeleteServiceInstanceRequest request,
																	DeleteServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder);
	}
}
