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

class InMemoryServiceInstanceStateRepositoryTest {

	private InMemoryServiceInstanceStateRepository stateRepository;

	@BeforeEach
	void setUp() {
		this.stateRepository = new InMemoryServiceInstanceStateRepository();
	}

	@Test
	void saveAndGet() {
		StepVerifier.create(stateRepository.saveState("foo", OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();
	}

	@Test
	void saveAndRemove() {
		StepVerifier.create(stateRepository.saveState("foo", OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.removeState("foo"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void updateState() {
		StepVerifier
			.create(stateRepository.saveState("foo-service", OperationState.IN_PROGRESS, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service"))
					.assertNext(serviceInstanceState -> {
						assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.IN_PROGRESS);
						assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
						assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
					})
					.verifyComplete();

		StepVerifier
			.create(stateRepository.saveState("foo-service", OperationState.SUCCEEDED, "bar"))
			.assertNext(serviceInstanceState -> {
				assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
				assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
				assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
			})
			.verifyComplete();

		StepVerifier.create(stateRepository.getState("foo-service"))
					.assertNext(serviceInstanceState -> {
						assertThat(serviceInstanceState.getOperationState()).isEqualTo(OperationState.SUCCEEDED);
						assertThat(serviceInstanceState.getDescription()).isEqualTo("bar");
						assertThat(serviceInstanceState.getLastUpdated()).isEqualToIgnoringSeconds(Calendar.getInstance().getTime());
					})
					.verifyComplete();
	}

	@Test
	void getWithUnknownServiceInstanceId() {
		StepVerifier.create(stateRepository.getState("foo"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}

	@Test
	void removeWithUnknownServiceInstanceId() {
		StepVerifier.create(stateRepository.removeState("foo"))
			.expectError(IllegalArgumentException.class)
			.verify();
	}
}