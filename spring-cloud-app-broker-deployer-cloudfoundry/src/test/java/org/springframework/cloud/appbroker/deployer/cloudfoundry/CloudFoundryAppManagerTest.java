/*
 * Copyright 2016-2019 the original author or authors
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void startApplication() {
		setupStubs();

		when(operationsApplications.start(any(org.cloudfoundry.operations.applications.StartApplicationRequest.class)))
			.thenReturn(Mono.empty());

		StartApplicationRequest request = StartApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.start(request))
			.verifyComplete();

		verify(operationsApplications).start(argThat(req -> "my-foo-app".equals(req.getName())));
		verifyNoMoreInteractions(operations);
	}

	@Test
	void startApplicationWithEmptyName() {
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(operations));

		StartApplicationRequest request = StartApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.start(request))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void stopNullApplication() {
		StepVerifier.create(appManager.stop(null))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void stopApplication() {
		setupStubs();

		when(operationsApplications.stop(any(org.cloudfoundry.operations.applications.StopApplicationRequest.class)))
			.thenReturn(Mono.empty());

		StopApplicationRequest request = StopApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.stop(request))
			.verifyComplete();

		verify(operationsApplications).stop(argThat(req -> "my-foo-app".equals(req.getName())));
		verifyNoMoreInteractions(operations);
	}

	@Test
	void stopApplicationWithEmptyName() {
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(operations));

		StopApplicationRequest request = StopApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.stop(request))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restartNullApplication() {
		StepVerifier.create(appManager.restart(null))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restartApplication() {
		setupStubs();

		when(operationsApplications.restart(any(org.cloudfoundry.operations.applications.RestartApplicationRequest.class)))
			.thenReturn(Mono.empty());

		RestartApplicationRequest request = RestartApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.restart(request))
			.verifyComplete();

		verify(operationsApplications).restart(argThat(req -> "my-foo-app".equals(req.getName())));
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restartApplicationWithEmptyName() {
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(operations));

		RestartApplicationRequest request = RestartApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.restart(request))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restageNullApplication() {
		StepVerifier.create(appManager.restage(null))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restageApplication() {
		setupStubs();

		when(operationsApplications.restage(any(org.cloudfoundry.operations.applications.RestageApplicationRequest.class)))
			.thenReturn(Mono.empty());

		RestageApplicationRequest request = RestageApplicationRequest.builder()
			.name("my-foo-app")
			.build();

		StepVerifier.create(appManager.restage(request))
			.verifyComplete();

		verify(operationsApplications).restage(argThat(req -> "my-foo-app".equals(req.getName())));
		verifyNoMoreInteractions(operations);
	}

	@Test
	void restageApplicationWithEmptyName() {
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(operations));

		RestageApplicationRequest request = RestageApplicationRequest.builder()
			.build();

		StepVerifier.create(appManager.restage(request))
			.verifyComplete();

		verifyZeroInteractions(operationsApplications);
		verifyNoMoreInteractions(operations);
	}

	private void setupStubs() {
		when(operations.applications()).thenReturn(operationsApplications);
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(operations));
	}
}