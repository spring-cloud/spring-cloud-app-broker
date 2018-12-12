/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Calendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.servicebroker.model.instance.OperationState;

import static org.assertj.core.api.Assertions.assertThat;

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
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
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
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.removeState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
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
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier
			.create(stateRepository.saveState("foo-service", "foo-binding",
				OperationState.SUCCEEDED, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service", "foo-binding"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
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
}