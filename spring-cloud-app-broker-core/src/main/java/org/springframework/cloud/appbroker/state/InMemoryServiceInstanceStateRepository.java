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

package org.springframework.cloud.appbroker.state;

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

public class InMemoryServiceInstanceStateRepository implements ServiceInstanceStateRepository {

	private volatile Map<String, ServiceInstanceState> states = new HashMap<>();

	@Override
	public Mono<ServiceInstanceState> saveState(String serviceInstanceId, OperationState state, String description) {
		ServiceInstanceState s = new ServiceInstanceState(state, description);
		this.states.put(serviceInstanceId, s);
		return Mono.just(s);
	}

	@Override
	public Mono<ServiceInstanceState> getState(String serviceInstanceId) {
		return containsState(serviceInstanceId) ?
			Mono.just(this.states.get(serviceInstanceId)) :
			Mono.error(new IllegalArgumentException("Unknown service instance ID " + serviceInstanceId));
	}

	@Override
	public Mono<ServiceInstanceState> removeState(String serviceInstanceId) {
		return containsState(serviceInstanceId) ?
			Mono.just(this.states.remove(serviceInstanceId)) :
			Mono.error(new IllegalArgumentException("Unknown service instance ID " + serviceInstanceId));
	}

	private boolean containsState(String serviceInstanceId) {
		return this.states.containsKey(serviceInstanceId);
	}
}
