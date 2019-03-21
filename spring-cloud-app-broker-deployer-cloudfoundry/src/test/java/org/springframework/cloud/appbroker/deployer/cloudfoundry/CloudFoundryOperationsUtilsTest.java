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

import java.util.Collections;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class CloudFoundryOperationsUtilsTest {

	private CloudFoundryOperationsUtils operationsUtils;

	private CloudFoundryOperations operations;

	@BeforeEach
	void setUp() {
		this.operations = DefaultCloudFoundryOperations.builder().build();
		this.operationsUtils = new CloudFoundryOperationsUtils(operations);
	}

	@Test
	void getOperationsWithEmptyProperties() {
		StepVerifier.create(operationsUtils.getOperations(Collections.emptyMap()))
			.expectNext(operations)
			.verifyComplete();
	}

	@Test
	void getOperationsWithProperties() {
		StepVerifier.create(operationsUtils.getOperations(Collections.singletonMap(DeploymentProperties.TARGET_PROPERTY_KEY, "foo-space1")))
			.assertNext(ops -> {
				String space = (String) ReflectionTestUtils.getField(ops, "space");
				assertThat(space).isEqualTo("foo-space1");
			})
			.verifyComplete();
	}

	@Test
	void getOperationsForSpace() {
		StepVerifier.create(operationsUtils.getOperationsForSpace("foo-space2"))
			.assertNext(ops -> {
				String space = (String) ReflectionTestUtils.getField(ops, "space");
				assertThat(space).isEqualTo("foo-space2");
			})
			.verifyComplete();
	}
}