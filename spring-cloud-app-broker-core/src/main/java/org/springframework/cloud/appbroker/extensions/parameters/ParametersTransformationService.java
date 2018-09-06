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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.ParametersTransformerSpec;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

public class ParametersTransformationService {
	private Map<String, ParametersTransformerFactory<?>> parametersTransformersByName = new HashMap<>();

	public ParametersTransformationService(List<ParametersTransformerFactory<?>> parametersTransformers) {
		parametersTransformers.forEach(parametersTransformer ->
			this.parametersTransformersByName.put(parametersTransformer.getName(), parametersTransformer));
	}

	public Mono<List<BackingApplication>> transformParameters(List<BackingApplication> backingApplications,
															  Map<String, Object> parameters) {
		return Flux.fromIterable(backingApplications)
			.map(BackingApplication::new)
			.flatMap(backingApplication -> {
				List<ParametersTransformerSpec> specs = getTransformerSpecsForApplication(backingApplication);

				return Flux.fromIterable(specs)
					.flatMap(spec -> {
						ParametersTransformerFactory<?> factory =
							getTransformerFactoryByName(spec.getName(), backingApplication.getName());
						ParametersTransformer transformer = getTransformerFromFactory(factory, spec);
						return transformParameters(transformer, backingApplication, parameters);
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

	private ParametersTransformerFactory<?> getTransformerFactoryByName(String transformerName, String applicationName) {
		if (parametersTransformersByName.containsKey(transformerName)) {
			return parametersTransformersByName.get(transformerName);
		} else {
			throw new ServiceBrokerException("Unknown parameters transformer " +
				transformerName + " configured for application " + applicationName + ". " +
				"Registered parameters transformers are " + parametersTransformersByName.keySet());
		}
	}

	private ParametersTransformer getTransformerFromFactory(ParametersTransformerFactory<?> factory,
															ParametersTransformerSpec spec) {
		return factory.createWithConfig(spec.getArgs());
	}

	private Mono<BackingApplication> transformParameters(ParametersTransformer transformer,
														 BackingApplication backingApplication,
														 Map<String, Object> parameters) {
		return transformer.transform(backingApplication, parameters);
	}
}
