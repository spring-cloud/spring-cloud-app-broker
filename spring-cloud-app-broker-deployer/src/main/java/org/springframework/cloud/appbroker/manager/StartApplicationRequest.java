/*
 * Copyright 2016-2019 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.manager;

import java.util.HashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

public class StartApplicationRequest {

	private final String name;

	private final Map<String, String> properties;

	StartApplicationRequest(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static StartApplicationRequestBuilder builder() {
		return new StartApplicationRequestBuilder();
	}

	public static class StartApplicationRequestBuilder {

		private String name;

		private final Map<String, String> properties = new HashMap<>();

		StartApplicationRequestBuilder() {
		}

		public StartApplicationRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public StartApplicationRequestBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public StartApplicationRequest build() {
			return new StartApplicationRequest(name, properties);
		}

	}
}
