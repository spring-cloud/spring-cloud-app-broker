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

package org.springframework.cloud.appbroker.service;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;

public interface UpdateServiceInstanceWorkflow {
	Mono<Void> update(UpdateServiceInstanceRequest request);

	default Mono<Boolean> accept(UpdateServiceInstanceRequest request) {
		return Mono.just(true);
	}

	default Mono<UpdateServiceInstanceResponseBuilder> buildResponse(UpdateServiceInstanceRequest request,
																	 UpdateServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder);
	}
}
