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
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;

import reactor.core.publisher.Mono;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

/**
 * Default implementation of {@link ServiceInstanceBindingStateRepository} meant for demonstration and testing purposes
 * only.
 * <p/>
 * WARNING: This implementation is not intended for production applications!
 */
public class InMemoryServiceInstanceBindingStateRepository implements ServiceInstanceBindingStateRepository {

	private final Map<BindingKey, ServiceInstanceState> states = new ConcurrentSkipListMap<>();

	@Override
	public Mono<ServiceInstanceState> saveState(String serviceInstanceId, String bindingId, OperationState state,
		String description) {
		return Mono.just(new BindingKey(serviceInstanceId, bindingId))
			.flatMap(bindingKey -> Mono
				.just(new ServiceInstanceState(state, description, new Timestamp(Instant.now().toEpochMilli())))
				.flatMap(
					serviceInstanceState -> Mono.fromCallable(() -> this.states.put(bindingKey, serviceInstanceState))
						.thenReturn(serviceInstanceState)));
	}

	@Override
	public Mono<ServiceInstanceState> getState(String serviceInstanceId, String bindingId) {
		return Mono.just(new BindingKey(serviceInstanceId, bindingId))
			.flatMap(bindingKey -> containsState(bindingKey)
				.flatMap(contains -> Mono.defer(() -> {
					if (contains) {
						return Mono.fromCallable(() -> this.states.get(bindingKey));
					}
					else {
						return Mono.error(new IllegalArgumentException("Unknown binding " + bindingKey));
					}
				})));
	}

	@Override
	public Mono<ServiceInstanceState> removeState(String serviceInstanceId, String bindingId) {
		return Mono.just(new BindingKey(serviceInstanceId, bindingId))
			.flatMap(bindingKey -> containsState(bindingKey)
				.flatMap(contains -> Mono.defer(() -> {
					if (contains) {
						return Mono.fromCallable(() -> this.states.remove(bindingKey));
					}
					else {
						return Mono.error(new IllegalArgumentException("Unknown binding " + bindingKey));
					}
				})));
	}

	private Mono<Boolean> containsState(BindingKey bindingKey) {
		return Mono.fromCallable(() -> this.states.containsKey(bindingKey));
	}

	private static class BindingKey implements Comparable<BindingKey> {

		private final String serviceInstanceId;

		private final String bindingId;

		public String getServiceInstanceId() {
			return this.serviceInstanceId;
		}

		public String getBindingId() {
			return this.bindingId;
		}

		public BindingKey(String serviceInstanceId, String bindingId) {
			this.serviceInstanceId = serviceInstanceId;
			this.bindingId = bindingId;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof BindingKey)) {
				return false;
			}
			BindingKey that = (BindingKey) obj;
			return Objects.equals(this.bindingId, that.bindingId) &&
				Objects.equals(this.serviceInstanceId, that.serviceInstanceId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(serviceInstanceId, bindingId);
		}

		@Override
		public String toString() {
			return "BindingKey{" +
				"serviceInstanceId='" + serviceInstanceId + '\'' +
				", bindingId='" + bindingId + '\'' +
				'}';
		}

		@Override
		public int compareTo(BindingKey other) {
			int compare = this.serviceInstanceId.compareTo(other.serviceInstanceId);
			return compare == 0 ? this.bindingId.compareTo(other.bindingId) : compare;
		}

	}

}
