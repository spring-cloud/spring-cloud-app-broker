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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingApplication;

import static org.assertj.core.api.Assertions.assertThat;

class EnvironmentMappingParametersTransformerFactoryTest {

	private ParametersTransformer<BackingApplication> transformer;

	@BeforeEach
	void setUp() {
		transformer = new EnvironmentMappingParametersTransformerFactory()
			.createWithConfig(config ->
				config.setInclude("parameter1,parameter2,parameter4,parameter5"));
	}

	@Test
	void parametersAreMappedToApplicationEnvironment() {
		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", Collections.singletonMap("key2", "value2"));
		parameters.put("parameter3", "value3");
		parameters.put("parameter4", Collections.singletonList("value4"));
		parameters.put("parameter5", Collections.singletonList(Collections.singletonMap("key5", "value5")));

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getEnvironment()).containsEntry("parameter1", "value1");
		assertThat(backingApplication.getEnvironment()).containsEntry("parameter2", "{\"key2\":\"value2\"}");
		assertThat(backingApplication.getEnvironment()).doesNotContainKey("parameter3");
		assertThat(backingApplication.getEnvironment()).containsEntry("parameter4", "[\"value4\"]");
		assertThat(backingApplication.getEnvironment()).containsEntry("parameter5", "[{\"key5\":\"value5\"}]");
	}

	@Test
	void parametersOverrideApplicationEnvironment() {
		BackingApplication backingApplication = BackingApplication.builder()
			.environment("parameter1", "config-value1")
			.environment("parameter3", "config-value3")
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("parameter1", "value1");
		parameters.put("parameter2", "value2");
		parameters.put("parameter3", "value3");

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getEnvironment()).containsEntry("parameter1", "value1");
		assertThat(backingApplication.getEnvironment()).containsEntry("parameter2", "value2");
		assertThat(backingApplication.getEnvironment()).containsEntry("parameter3", "config-value3");
	}

}
