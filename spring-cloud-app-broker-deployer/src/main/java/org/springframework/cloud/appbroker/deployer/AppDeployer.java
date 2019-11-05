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

package org.springframework.cloud.appbroker.deployer;

import reactor.core.publisher.Mono;

public interface AppDeployer {

	default Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
		return Mono.empty();
	}

	default Mono<UpdateApplicationResponse> update(UpdateApplicationRequest request) {
		return Mono.empty();
	}

	default Mono<UndeployApplicationResponse> undeploy(UndeployApplicationRequest request) {
		return Mono.empty();
	}

	default Mono<GetApplicationResponse> get(GetApplicationRequest request) {
		return Mono.empty();
	}

	default Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return Mono.empty();
	}

	default Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		return Mono.empty();
	}

	default Mono<CreateServiceKeyResponse> createServiceKey(CreateServiceKeyRequest request) {
		return Mono.empty();
	}

	default Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return Mono.empty();
	}

	default Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return Mono.empty();
	}
	default Mono<DeleteServiceKeyResponse> deleteServiceKey(DeleteServiceKeyRequest request) {
		return Mono.empty();
	}

}
