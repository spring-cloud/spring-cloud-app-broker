/*
 * Copyright 2002-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.Applications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.manager.RestageApplicationRequest;
import org.springframework.cloud.appbroker.manager.RestartApplicationRequest;
import org.springframework.cloud.appbroker.manager.StartApplicationRequest;
import org.springframework.cloud.appbroker.manager.StopApplicationRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class CloudFoundryAppManagerTest {

	private CloudFoundryAppManager appManager;

	@Mock
	private Applications operationsApplications;

	@Mock
	private CloudFoundryOperations operations;

	@Mock
	private CloudFoundryOperationsUtils operationsUtils;

	@BeforeEach
	void setUp() {
		this.appManager = new CloudFoundryAppManager(operationsUtils);
	}

	@Test
	void startNullApplication() {
		StepVerifier.create(appManager.start(null))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void startApplication() {
		setupStubs();

		given(operationsApplications.start(any(org.cloudfoundry.operations.applications.StartApplicationRequest.class)))
			.willReturn(Mono.empty());

		StartApplicationRequest request = StartApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.start(request))
			.verifyComplete();

		then(operationsApplications).should().start(argThat(req -> "my-foo-app".equals(req.getName())));
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void startApplicationWithEmptyName() {
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(operations));

		StartApplicationRequest request = StartApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.start(request))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void stopNullApplication() {
		StepVerifier.create(appManager.stop(null))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void stopApplication() {
		setupStubs();

		given(operationsApplications.stop(any(org.cloudfoundry.operations.applications.StopApplicationRequest.class)))
			.willReturn(Mono.empty());

		StopApplicationRequest request = StopApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.stop(request))
			.verifyComplete();

		then(operationsApplications).should().stop(argThat(req -> "my-foo-app".equals(req.getName())));
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void stopApplicationWithEmptyName() {
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(operations));

		StopApplicationRequest request = StopApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.stop(request))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restartNullApplication() {
		StepVerifier.create(appManager.restart(null))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restartApplication() {
		setupStubs();

		given(operationsApplications
			.restart(any(org.cloudfoundry.operations.applications.RestartApplicationRequest.class)))
			.willReturn(Mono.empty());

		RestartApplicationRequest request = RestartApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.restart(request))
			.verifyComplete();

		then(operationsApplications).should().restart(argThat(req -> "my-foo-app".equals(req.getName())));
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restartApplicationWithEmptyName() {
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(operations));

		RestartApplicationRequest request = RestartApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.restart(request))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restageNullApplication() {
		StepVerifier.create(appManager.restage(null))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restageApplication() {
		setupStubs();

		given(operationsApplications
			.restage(any(org.cloudfoundry.operations.applications.RestageApplicationRequest.class)))
			.willReturn(Mono.empty());

		RestageApplicationRequest request = RestageApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.restage(request))
			.verifyComplete();

		then(operationsApplications).should().restage(argThat(req -> "my-foo-app".equals(req.getName())));
		then(operations).shouldHaveNoMoreInteractions();
	}

	@Test
	void restageApplicationWithEmptyName() {
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(operations));

		RestageApplicationRequest request = RestageApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.restage(request))
			.verifyComplete();

		then(operationsApplications).shouldHaveNoInteractions();
		then(operations).shouldHaveNoMoreInteractions();
	}

	private void setupStubs() {
		given(operations.applications()).willReturn(operationsApplications);
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(operations));
	}

}
