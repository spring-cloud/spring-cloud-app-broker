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

package org.springframework.cloud.appbroker.extensions.parameters;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.BackingApplications;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParametersTransformationService {
	private Map<String, ParametersTransformer> parametersTransformersByName = new HashMap<>();

	public ParametersTransformationService(List<ParametersTransformer> parametersTransformers) {
		parametersTransformers.forEach(parametersTransformer ->
			this.parametersTransformersByName.put(parametersTransformer.getName(), parametersTransformer));
	}

	public Mono<BackingApplications> transformParameters(BackingApplications backingApplications,
														 Map<String, Object> parameters) {
		return Flux.fromIterable(backingApplications)
			.flatMap(backingApplication ->
				Flux.fromIterable(getTransformers(backingApplication))
					.flatMap(transformerName ->
						transform(transformerName, backingApplication, parameters)
					))
			.then(Mono.just(backingApplications));
	}

	private List<String> getTransformers(BackingApplication backingApplication) {
		return backingApplication.getParametersTransformers() == null
			? Collections.emptyList()
			: backingApplication.getParametersTransformers();
	}

	private Mono<BackingApplication> transform(String transformerName,
											   BackingApplication backingApplication,
											   Map<String, Object> parameters) {
		return getTransformerByName(transformerName, backingApplication.getName())
			.flatMap(transformer -> transformer.transform(backingApplication, parameters));
	}

	private Mono<ParametersTransformer> getTransformerByName(String transformerName, String applicationName) {
		return Mono.defer(() -> {
			if (parametersTransformersByName.containsKey(transformerName)) {
				return Mono.just(parametersTransformersByName.get(transformerName));
			} else {
				return Mono.error(new ServiceBrokerException("Unknown parameters transformer " +
					transformerName + " configured for application " + applicationName + ". " +
					"Registered parameters transformers are " + parametersTransformersByName.keySet()));
			}
		});
	}
}
