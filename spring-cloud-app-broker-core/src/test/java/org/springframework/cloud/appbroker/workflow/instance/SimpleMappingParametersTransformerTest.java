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
import org.springframework.cloud.appbroker.extensions.parameters.SimpleMappingParametersTransformer;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleMappingParametersTransformerTest {
	private final SimpleMappingParametersTransformer transformer = new SimpleMappingParametersTransformer();

	@Test
	void parametersAreMappedToApplicationEnvironment() {
		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		parameters.forEach((key, value) ->
			assertThat(backingApplication.getEnvironment()).containsEntry(key, value.toString())
		);
	}

	@Test
	void parametersOverrideApplicationEnvironment() {
		BackingApplication backingApplication = BackingApplication.builder()
			.environment("parameter1", "config-value1")
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		parameters.forEach((key, value) -> {
				assertThat(backingApplication.getEnvironment()).containsEntry(key, value.toString());
				assertThat(backingApplication.getEnvironment()).containsEntry(key, value.toString());
			}
		);
	}

}