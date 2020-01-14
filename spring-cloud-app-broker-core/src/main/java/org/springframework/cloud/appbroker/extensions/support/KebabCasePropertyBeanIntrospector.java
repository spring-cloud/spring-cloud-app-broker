/*
 * Copyright 2002-2020 the original author or authors.
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
package org.springframework.cloud.appbroker.extensions.support;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Locale;

import org.apache.commons.beanutils.BeanIntrospector;
import org.apache.commons.beanutils.DefaultBeanIntrospector;
import org.apache.commons.beanutils.IntrospectionContext;
import reactor.util.Logger;
import reactor.util.Loggers;

/**
 * An implementation of the {@link BeanIntrospector} interface that provides property descriptors following the
 * kebab-case convention.
 * <p>
 * This implementation is intended to collaborate with a {@link DefaultBeanIntrospector} object. Best results are
 * achieved by adding this instance as custom {@link BeanIntrospector} after the {@link DefaultBeanIntrospector}
 * object.
 */
public class KebabCasePropertyBeanIntrospector implements BeanIntrospector {

	private static final Logger LOG = Loggers.getLogger(KebabCasePropertyBeanIntrospector.class);

	private static final String WRITE_METHOD_PREFIX = "set";

	/**
	 * Performs introspection. This method scans the current class's methods for property write methods add adds a
	 * property descriptor using the kebab-case naming convention to match each property descriptor that uses the
	 * camel-case Java Bean convention.
	 *
	 * @param context the introspection context
	 */
	@Override
	public void introspect(final IntrospectionContext context) {
		for (final Method m : context.getTargetClass().getMethods()) {
			if (m.getName().startsWith(WRITE_METHOD_PREFIX)) {
				final String propertyName = camelCasePropertyName(m);
				final PropertyDescriptor pd = context.getPropertyDescriptor(propertyName);
				try {
					if (pd != null) {
						context.addPropertyDescriptor(createPropertyDescriptor(m));
					}
				}
				catch (final IntrospectionException e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("Error when creating PropertyDescriptor for method '{}'. " +
							"This property will be ignored. {}", m, e);
					}
				}
			}
		}
	}

	/**
	 * Derives the camel-case name of a property from the given set method.
	 *
	 * @param m the method
	 * @return the corresponding property name
	 */
	private String camelCasePropertyName(final Method m) {
		final String methodName = m.getName().substring(WRITE_METHOD_PREFIX.length());
		return methodName.length() > 1 ?
			Introspector.decapitalize(methodName) :
			methodName.toLowerCase(Locale.ENGLISH);
	}

	/**
	 * Derives the kebab-case name of a property from the given set method.
	 *
	 * @param m the method
	 * @return the corresponding property name
	 */
	private String kebabCasePropertyName(final Method m) {
		final String methodName = camelCasePropertyName(m);

		StringBuilder builder = new StringBuilder();
		for (char c : methodName.toCharArray()) {
			if (Character.isUpperCase(c)) {
				builder.append('-').append(Character.toLowerCase(c));
			}
			else {
				builder.append(c);
			}
		}

		return builder.toString();
	}

	/**
	 * Creates a property descriptor for a property.
	 *
	 * @param m the set method for the property
	 * @return the descriptor
	 * @throws IntrospectionException if an error occurs
	 */
	private PropertyDescriptor createPropertyDescriptor(final Method m) throws IntrospectionException {
		String propertyName = kebabCasePropertyName(m);
		return new PropertyDescriptor(propertyName, null, m);
	}

}
