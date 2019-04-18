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

package org.springframework.cloud.appbroker.extensions.parameters;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingService;

import static org.assertj.core.api.Assertions.assertThat;

class ParameterMappingParametersTransformerFactoryTest {

	private ParametersTransformer<BackingService> transformer;

	@BeforeEach
	void setUp() {
		transformer = new ParameterMappingParametersTransformerFactory()
			.createWithConfig(config ->
				config.setInclude("parameter1,parameter2"));
	}

	@Test
	void parametersOverrideApplicationEnvironment() {
		Map<String, Object> inputParameters = new HashMap<>();
		inputParameters.put("parameter1", "value1");
		inputParameters.put("parameter2", "value2");
		inputParameters.put("parameter3", "value3");


		Map<String, Object> expectedParameters = new HashMap<>();
		expectedParameters.put("parameter1", "value1");
		expectedParameters.put("parameter2", "value2");

		BackingService backingService =
			BackingService.builder()
						  .parameters(expectedParameters)
						  .build();

		StepVerifier
			.create(transformer.transform(backingService, inputParameters))
			.expectNext(backingService)
			.verifyComplete();

		assertThat(backingService.getParameters()).containsEntry("parameter1", "value1");
		assertThat(backingService.getParameters()).containsEntry("parameter2", "value2");
		assertThat(backingService.getParameters()).doesNotContainEntry("parameter3", "value3");
	}

}