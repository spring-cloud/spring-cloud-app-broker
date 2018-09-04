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

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public abstract class ConfigurationBeanUtils {

	public static <T> T instantiate(Class<T> cls) {
		return org.springframework.beans.BeanUtils.instantiateClass(cls);
	}

	public static <T> void populate(T targetObject, Map<String, Object> properties) {
		T target = getTargetObject(targetObject);

		try {
			org.apache.commons.beanutils.BeanUtils.populate(target, properties);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new IllegalArgumentException("Failed to populate target of type " + targetObject.getClass()
				+ " with properties " + properties, e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T getTargetObject(Object candidate) {
		try {
			if (AopUtils.isAopProxy(candidate) && (candidate instanceof Advised)) {
				return (T) ((Advised) candidate).getTargetSource().getTarget();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to unwrap proxied object", e);
		}
		return (T) candidate;
	}
}