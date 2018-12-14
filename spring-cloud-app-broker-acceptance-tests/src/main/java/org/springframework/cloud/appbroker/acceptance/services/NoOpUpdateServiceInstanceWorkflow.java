/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.acceptance.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NoOpUpdateServiceInstanceWorkflow implements UpdateServiceInstanceWorkflow {
	@Value("${spring.cloud.openservicebroker.catalog.services[1].id}")
	private String backingServiceId;

	@Override
	public Mono<Void> update(UpdateServiceInstanceRequest request) {
		return Mono.empty();
	}

	@Override
	public Mono<Boolean> accept(UpdateServiceInstanceRequest request) {
		return Mono.just(request.getServiceDefinitionId().equals(backingServiceId));
	}

	@Override
	public Mono<UpdateServiceInstanceResponseBuilder> buildResponse(UpdateServiceInstanceRequest request,
																	UpdateServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder);
	}
}
