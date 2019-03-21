/*
 * Copyright 2016-2018 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.BackingServices;
import org.springframework.cloud.appbroker.deployer.ParametersTransformerSpec;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

import static org.assertj.core.api.Assertions.assertThat;

class BackingServicesParametersTransformationServiceTest {

	@Test
	void transformParametersWithNoBackingServices() {
		BackingServicesParametersTransformationService service =
			new BackingServicesParametersTransformationService(Collections.emptyList());

		BackingServices backingServices = BackingServices.builder()
														 .build();

		StepVerifier
			.create(service.transformParameters(backingServices, new HashMap<>()))
			.expectNext(backingServices)
			.verifyComplete();
	}

	@Test
	void transformParametersWithNoTransformers() {
		BackingServicesParametersTransformationService service =
			new BackingServicesParametersTransformationService(Collections.emptyList());

		BackingServices backingServices = BackingServices
			.builder()
			.backingService(BackingService.builder().build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingServices, new HashMap<>()))
			.expectNext(backingServices)
			.verifyComplete();
	}

	@Test
	void transformParametersWithUnknownTransformer() {
		BackingServicesParametersTransformationService service =
			new BackingServicesParametersTransformationService(Collections.emptyList());

		BackingServices backingServices = BackingServices
			.builder()
			.backingService(BackingService.builder()
										  .name("misconfigured-service")
										  .parameterTransformers(ParametersTransformerSpec
											  .builder()
											  .name("unknown-transformer")
											  .build())
										  .build())
			.build();

		StepVerifier
			.create(service.transformParameters(backingServices, new HashMap<>()))
			.expectErrorSatisfies(e -> assertThat(e)
				.isInstanceOf(ServiceBrokerException.class)
				.hasMessageContaining("unknown-transformer"))
			.verify();
	}
}