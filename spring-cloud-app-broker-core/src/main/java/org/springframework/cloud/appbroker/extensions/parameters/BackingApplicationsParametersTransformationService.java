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

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.ParametersTransformerSpec;
import org.springframework.cloud.appbroker.extensions.ExtensionLocator;

public class BackingApplicationsParametersTransformationService {

	private final ExtensionLocator<ParametersTransformer<BackingApplication>> locator;

	public BackingApplicationsParametersTransformationService(
		List<ParametersTransformerFactory<BackingApplication, ?>> factories) {
		locator = new ExtensionLocator<>(factories);
	}

	public Mono<List<BackingApplication>> transformParameters(List<BackingApplication> backingApplications,
															  Map<String, Object> parameters) {
		return Flux.fromIterable(backingApplications)
				   .flatMap(backingApplication -> {
					   List<ParametersTransformerSpec> specs = getTransformerSpecsForApplication(backingApplication);

					   return Flux.fromIterable(specs)
								  .flatMap(spec -> {
									  ParametersTransformer<BackingApplication> transformer = locator.getByName(spec.getName(), spec.getArgs());
									  return transformer.transform(backingApplication, parameters);
								  })
								  .then(Mono.just(backingApplication));
				   })
				   .collectList();
	}

	private List<ParametersTransformerSpec> getTransformerSpecsForApplication(BackingApplication backingApplication) {
		return backingApplication.getParametersTransformers() == null
			? Collections.emptyList()
			: backingApplication.getParametersTransformers();
	}
}
