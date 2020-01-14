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

package org.springframework.cloud.appbroker.state;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

public class InMemoryServiceInstanceStateRepository implements ServiceInstanceStateRepository {

	private final Map<String, ServiceInstanceState> states = new ConcurrentHashMap<>();

	@Override
	public Mono<ServiceInstanceState> saveState(String serviceInstanceId, OperationState state, String description) {
		return Mono.just(new ServiceInstanceState(state, description, new Timestamp(Instant.now().toEpochMilli())))
			.flatMap(serviceInstanceState -> Mono
				.fromCallable(() -> this.states.put(serviceInstanceId, serviceInstanceState))
				.thenReturn(serviceInstanceState));
	}

	@Override
	public Mono<ServiceInstanceState> getState(String serviceInstanceId) {
		return containsState(serviceInstanceId)
			.flatMap(contains -> Mono.defer(() -> {
				if (contains) {
					return Mono.fromCallable(() -> this.states.get(serviceInstanceId));
				}
				else {
					return Mono.error(new IllegalArgumentException("Unknown service instance ID " + serviceInstanceId));
				}
			}));
	}

	@Override
	public Mono<ServiceInstanceState> removeState(String serviceInstanceId) {
		return containsState(serviceInstanceId)
			.flatMap(contains -> Mono.defer(() -> {
				if (contains) {
					return Mono.fromCallable(() -> this.states.remove(serviceInstanceId));
				}
				else {
					return Mono.error(new IllegalArgumentException("Unknown service instance ID " + serviceInstanceId));
				}
			}));
	}

	private Mono<Boolean> containsState(String serviceInstanceId) {
		return Mono.fromCallable(() -> this.states.containsKey(serviceInstanceId));
	}

}
