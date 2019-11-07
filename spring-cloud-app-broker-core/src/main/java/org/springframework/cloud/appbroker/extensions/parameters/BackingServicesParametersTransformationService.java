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
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingService;
import org.springframework.cloud.appbroker.deployer.ParametersTransformerSpec;
import org.springframework.cloud.appbroker.extensions.ExtensionLocator;

public class BackingServicesParametersTransformationService {

	private final ExtensionLocator<ParametersTransformer<BackingService>> locator;

	public BackingServicesParametersTransformationService(
		List<ParametersTransformerFactory<BackingService, ?>> factories) {
		locator = new ExtensionLocator<>(factories);
	}

	public Mono<List<BackingService>> transformParameters(List<BackingService> backingServices,
		Map<String, Object> parameters) {
		return Flux.fromIterable(backingServices)
			.flatMap(backingService -> {
				List<ParametersTransformerSpec> specs = getTransformerSpecsForService(backingService);

				return Flux.fromIterable(specs)
					.flatMap(spec -> {
						ParametersTransformer<BackingService> transformer = locator
							.getByName(spec.getName(), spec.getArgs());
						return transformer.transform(backingService, parameters);
					})
					.then(Mono.just(backingService));
			})
			.collectList();
	}

	private List<ParametersTransformerSpec> getTransformerSpecsForService(BackingService backingService) {
		return backingService.getParametersTransformers() == null
			? Collections.emptyList()
			: backingService.getParametersTransformers();
	}

}
