/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class DefaultBackingSpaceManagementServiceTest {

	@Mock
	private DeployerClient deployerClient;

	private BackingSpaceManagementService backingSpaceManagementService;

	@BeforeEach
	void setUp() {
		backingSpaceManagementService = new DefaultBackingSpaceManagementService(deployerClient);
	}

	@Test
	@SuppressWarnings("UnassignedFluxMonoInstance")
	void deleteSpace() {
		final String spaceName1 = "space1";
		final String spaceName2 = "space2";
		doReturn(Mono.just("returned-space-1"))
			.when(deployerClient).deleteSpace(spaceName1);
		doReturn(Mono.just("returned-space-2"))
			.when(deployerClient).deleteSpace(spaceName2);

		List<String> expectedValues = new ArrayList<>();
		expectedValues.add("returned-space-1");
		expectedValues.add("returned-space-2");

		StepVerifier.create(backingSpaceManagementService.deleteTargetSpaces(asList(spaceName1, spaceName2)))
			// deployments are run in parallel, so the order of completion is not predictable
			// ensure that both expected signals are sent in any order
			.expectNextMatches(expectedValues::remove)
			.expectNextMatches(expectedValues::remove)
			.verifyComplete();
	}

}
