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

package org.springframework.cloud.appbroker.workflow.instance;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.ParametersTransformer;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ParametersTransformationServiceTest {
	@Test
	void transformParametersWithNoBackingApps() {
		ParametersTransformationService service = new ParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
			.build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	@Test
	void transformParametersWithNoTransformers() {
		ParametersTransformationService service = new ParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(BackingApplication.builder().build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectNext(backingApplications)
			.verifyComplete();
	}

	@Test
	void transformParametersWithUnknownTransformer() {
		ParametersTransformationService service = new ParametersTransformationService(Collections.emptyList());

		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(BackingApplication.builder()
				.name("misconfigured-app")
				.parameterTransformers("unknown-transformer")
				.build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingApplications, new HashMap<>()))
			.expectError(ServiceBrokerException.class)
			.verify();
	}

	@Test
	void transformParametersWithTransformers() {
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("key1", "value1");
		parameters.put("key2", "value2");

		BackingApplication app1 = BackingApplication.builder()
			.name("app1")
			.parameterTransformers("transformer1")
			.build();
		BackingApplication app2 = BackingApplication.builder()
			.name("app2")
			.parameterTransformers("transformer1", "transformer2")
			.build();
		BackingApplications backingApplications = BackingApplications.builder()
			.backingApplication(app1)
			.backingApplication(app2)
			.build();

		ParametersTransformer transformer1 = mock(ParametersTransformer.class);
		given(transformer1.getName()).willReturn("transformer1");
		given(transformer1.transform(eq(app1), eq(parameters)))
			.willReturn(Mono.just(app1));
		given(transformer1.transform(eq(app2), eq(parameters)))
			.willReturn(Mono.just(app2));

		ParametersTransformer transformer2 = mock(ParametersTransformer.class);
		given(transformer2.getName()).willReturn("transformer2");
		given(transformer2.transform(eq(app2), eq(parameters)))
			.willReturn(Mono.just(app2));

		ParametersTransformationService service = new ParametersTransformationService(
			Arrays.asList(transformer1, transformer2));

		StepVerifier
			.create(service.transformParameters(backingApplications, parameters))
			.expectNext(backingApplications)
			.verifyComplete();
	}
}