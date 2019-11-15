/*
 * Copyright 2002-2019 the original author or authors
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

package org.springframework.cloud.appbroker.manager;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class ManagementClientTest {

	private ManagementClient managementClient;

	@Mock
	private AppManager appManager;

	private BackingApplication backingApplication;

	@BeforeEach
	void setUp() {
		this.managementClient = new ManagementClient(appManager);

		this.backingApplication = BackingApplication.builder()
			.name("foo-app")
			.property("foo", "bar")
			.build();
	}

	@Test
	void startApplication() {
		given(appManager.start(any(StartApplicationRequest.class)))
			.willReturn(Mono.empty());

		StepVerifier.create(managementClient.start(backingApplication))
			.verifyComplete();

		verify(appManager).start(argThat(request -> "foo-app".equals(request.getName()) &&
			Collections.singletonMap("foo", "bar").equals(request.getProperties())));
		verifyNoMoreInteractions(appManager);
	}

	@Test
	void startNullApplication() {
		StepVerifier.create(managementClient.start(null))
			.verifyComplete();

		verifyNoInteractions(appManager);
	}

	@Test
	void stopApplication() {
		given(appManager.stop(any(StopApplicationRequest.class)))
			.willReturn(Mono.empty());

		StepVerifier.create(managementClient.stop(backingApplication))
			.verifyComplete();

		verify(appManager).stop(argThat(request -> "foo-app".equals(request.getName()) &&
			Collections.singletonMap("foo", "bar").equals(request.getProperties())));
		verifyNoMoreInteractions(appManager);
	}

	@Test
	void stopNullApplication() {
		StepVerifier.create(managementClient.stop(null))
			.verifyComplete();

		verifyNoInteractions(appManager);
	}

	@Test
	void restartApplication() {
		given(appManager.restart(any(RestartApplicationRequest.class)))
			.willReturn(Mono.empty());

		StepVerifier.create(managementClient.restart(backingApplication))
			.verifyComplete();

		verify(appManager).restart(argThat(request -> "foo-app".equals(request.getName()) &&
			Collections.singletonMap("foo", "bar").equals(request.getProperties())));
		verifyNoMoreInteractions(appManager);
	}

	@Test
	void restartNullApplication() {
		StepVerifier.create(managementClient.restart(null))
			.verifyComplete();

		verifyNoInteractions(appManager);
	}

	@Test
	void restageApplication() {
		given(appManager.restage(any(RestageApplicationRequest.class)))
			.willReturn(Mono.empty());

		StepVerifier.create(managementClient.restage(backingApplication))
			.verifyComplete();

		verify(appManager).restage(argThat(request -> "foo-app".equals(request.getName()) &&
			Collections.singletonMap("foo", "bar").equals(request.getProperties())));
		verifyNoMoreInteractions(appManager);
	}

	@Test
	void restageNullApplication() {
		StepVerifier.create(managementClient.restage(null))
			.verifyComplete();

		verifyNoInteractions(appManager);
	}

}
