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

import org.springframework.cloud.appbroker.extensions.support.ConfigurationBeanUtils;

import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractExtensionFactory<T, C> implements ExtensionFactory<T, C> {
	private Class<C> configClass;

	@SuppressWarnings("unchecked")
	public AbstractExtensionFactory() {
		this((Class<C>) Object.class);
	}

	public AbstractExtensionFactory(Class<C> configClass) {
		this.configClass = configClass;
	}

	@Override
	public abstract T create(C config);

	public T createWithConfig(Consumer<C> consumer) {
		C config = ConfigurationBeanUtils.instantiate(this.configClass);
		consumer.accept(config);
		return create(config);
	}

	public T createWithConfig(Map<String, Object> args) {
		C config = ConfigurationBeanUtils.instantiate(this.configClass);
		ConfigurationBeanUtils.populate(config, args);
		return create(config);
	}

	protected String getShortName(Class<?> cls) {
		return getClass().getSimpleName().replace(cls.getSimpleName(), "");
	}
}
