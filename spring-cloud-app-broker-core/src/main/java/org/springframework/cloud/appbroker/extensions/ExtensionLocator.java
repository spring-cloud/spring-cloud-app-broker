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

package org.springframework.cloud.appbroker.extensions;

import org.springframework.cloud.servicebroker.exception.ServiceBrokerException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtensionLocator<T> {
	private final Map<String, ExtensionFactory<T, ?>> factoriesByName = new HashMap<>();

	public ExtensionLocator(List<? extends ExtensionFactory<T, ?>> factories) {
		factories.forEach(extension -> this.factoriesByName.put(extension.getName(), extension));
	}

	public T getByName(String name) {
		return getByName(name, Collections.emptyMap());
	}

	public T getByName(String name, Map<String, Object> args) {
		ExtensionFactory<T, ?> factory = getFactoryByName(name);
		return getExtensionFromFactory(factory, args);
	}

	private ExtensionFactory<T, ?> getFactoryByName(String name) {
		if (factoriesByName.containsKey(name)) {
			return factoriesByName.get(name);
		} else {
			throw new ServiceBrokerException("Unknown extension " + name + ". " +
				"Registered extensions are " + factoriesByName.keySet());
		}
	}

	private T getExtensionFromFactory(ExtensionFactory<T, ?> factory,
									  Map<String, Object> args) {
		return factory.createWithConfig(args);
	}
}
