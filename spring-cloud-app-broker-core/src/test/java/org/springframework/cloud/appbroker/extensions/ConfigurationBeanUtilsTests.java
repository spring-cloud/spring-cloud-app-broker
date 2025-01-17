/*
 * Copyright 2016-2020 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.appbroker.extensions.support.ConfigurationBeanUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationBeanUtilsTests {

	private final TestProperties targetObject = new TestProperties();

	private final Map<String, Object> properties = new HashMap<>();

	@Test
	void populateWithCamelCaseProperties() {
		this.properties.put("stringValue", "value");
		this.properties.put("intValue", 41);
		this.properties.put("booleanValue", true);

		ConfigurationBeanUtils.populate(this.targetObject, this.properties);

		assertValuesPopulated(this.targetObject);
	}

	@Test
	void populateWithKebabCaseProperties() {
		this.properties.put("string-value", "value");
		this.properties.put("int-value", 41);
		this.properties.put("boolean-value", true);

		ConfigurationBeanUtils.populate(this.targetObject, this.properties);

		assertValuesPopulated(this.targetObject);
	}

	@Test
	void populateWithEmptyProperties() {
		ConfigurationBeanUtils.populate(this.targetObject, this.properties);

		assertValuesNotPopulated(this.targetObject);
	}

	@Test
	void populateWithNullProperties() {
		ConfigurationBeanUtils.populate(this.targetObject, null);

		assertValuesNotPopulated(this.targetObject);
	}

	private void assertValuesPopulated(TestProperties targetObject) {
		assertThat(targetObject.getStringValue()).isEqualTo("value");
		assertThat(targetObject.getIntValue()).isEqualTo(41);
		assertThat(targetObject.isBooleanValue()).isEqualTo(true);
	}

	private void assertValuesNotPopulated(TestProperties targetObject) {
		assertThat(targetObject.getStringValue()).isNull();
		assertThat(targetObject.getIntValue()).isEqualTo(0);
		assertThat(targetObject.isBooleanValue()).isEqualTo(false);
	}

	@SuppressWarnings("unused")
	public static class TestProperties {

		private String stringValue;

		private int intValue;

		private boolean booleanValue;

		public String getStringValue() {
			return this.stringValue;
		}

		public void setStringValue(String stringValue) {
			this.stringValue = stringValue;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

		public boolean isBooleanValue() {
			return this.booleanValue;
		}

		public void setBooleanValue(boolean booleanValue) {
			this.booleanValue = booleanValue;
		}

	}

}
