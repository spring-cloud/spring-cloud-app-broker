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

package org.springframework.cloud.appbroker.extensions;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionLocator<T> {
	private final HashMap<String, ExtensionFactory<T, ?>> factoriesByName = new HashMap<>();

	public ExtensionLocator(List<? extends ExtensionFactory<T, ?>> factories) {
		factories.forEach(parametersTransformer ->
			this.factoriesByName.put(parametersTransformer.getName(), parametersTransformer));
	}

	public T getByName(String name, Map<String, Object> args) {
		ExtensionFactory<T, ?> factory = getTransformerFactoryByName(name);
		return getTransformerFromFactory(factory, args);
	}

	private ExtensionFactory<T, ?> getTransformerFactoryByName(String transformerName) {
		if (factoriesByName.containsKey(transformerName)) {
			return factoriesByName.get(transformerName);
		} else {
			throw new ServiceBrokerException("Unknown parameters transformer " + transformerName + ". " +
				"Registered parameters transformers are " + factoriesByName.keySet());
		}
	}

	private T getTransformerFromFactory(ExtensionFactory<T, ?> factory,
										Map<String, Object> args) {
		return factory.createWithConfig(args);
	}
}
