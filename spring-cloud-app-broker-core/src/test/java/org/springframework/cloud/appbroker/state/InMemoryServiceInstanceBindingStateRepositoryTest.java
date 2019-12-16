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

package org.springframework.cloud.appbroker.state;

import java.util.Calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
// See https://github.com/spring-cloud/spring-cloud-app-broker/issues/313
class InMemoryServiceInstanceBindingStateRepositoryTest {

	private InMemoryServiceInstanceBindingStateRepository stateRepository;

	@BeforeEach
	void setUp() {
		this.stateRepository = new InMemoryServiceInstanceBindingStateRepository();
	}

	@Test
	void saveAndGet() {
		StepVerifier
			.create(stateRepository.saveState("foo-service", "foo-binding",
				OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();
	}

	@Test
	void saveAndRemove() {
		StepVerifier
			.create(stateRepository.saveState("foo-service", "foo-binding",
				OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.removeState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void updateState() {
		StepVerifier
			.create(stateRepository.saveState("foo-service", "foo-binding",
				OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier
			.create(stateRepository.saveState("foo-service", "foo-binding",
				OperationState.SUCCEEDED, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated())
					.isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();
	}

	@Test
	void getWithNullBindingKey() {
		StepVerifier.create(stateRepository.getState(null, null))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void getWithUnknownBindingKey() {
		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void removeWithUnknownBindingKey() {
		StepVerifier.create(stateRepository.removeState("foo-service", "foo-binding"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void ensureConcurrency() {
		StepVerifier.create(
			Flux.range(0, 100_000)
				.parallel(25)
				.runOn(Schedulers.newParallel("parallel-test", 25))
				.flatMap(value ->
					stateRepository
						.saveState(
							"foo-service" + value,
							"foo-binding" + value,
							OperationState.IN_PROGRESS,
							"bar")
						.then(stateRepository.removeState("foo-service" + value, "foo-binding" + value))))
			.expectNextCount(100_000)
			.verifyComplete();
	}

}
