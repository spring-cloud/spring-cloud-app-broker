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

import org.springframework.cloud.appbroker.deployer.BackingApplication;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMappingParametersTransformerFactoryTest {

	private ParametersTransformer<BackingApplication> transformer;

	@BeforeEach
	void setUp() {
		transformer = new PropertyMappingParametersTransformerFactory()
			.createWithConfig(config ->
				config.setInclude("count,memory"));
	}

	@Test
	void parametersAreMappedToApplicationProperties() {
		BackingApplication backingApplication = BackingApplication.builder()
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("count", 2);
		parameters.put("memory", "2G");
		parameters.put("health-check-type", "none");

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getProperties()).containsEntry("count", "2");
		assertThat(backingApplication.getProperties()).containsEntry("memory", "2G");
		assertThat(backingApplication.getProperties()).doesNotContainKey("health-check-type");
	}

	@Test
	void parametersOverrideApplicationProperties() {
		BackingApplication backingApplication = BackingApplication.builder()
			.property("count", "1")
			.property("memory", "1G")
			.property("health-check-type", "http")
			.property("health-check-http-endpoint", "/myhealth")
			.build();

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("count", 2);
		parameters.put("memory", "2G");
		parameters.put("health-check-type", "none");

		StepVerifier
			.create(transformer.transform(backingApplication, parameters))
			.expectNext(backingApplication)
			.verifyComplete();

		assertThat(backingApplication.getProperties()).containsEntry("count", "2");
		assertThat(backingApplication.getProperties()).containsEntry("memory", "2G");
		assertThat(backingApplication.getProperties()).containsEntry("health-check-type", "http");
		assertThat(backingApplication.getProperties()).containsEntry("health-check-http-endpoint", "/myhealth");
	}

}
