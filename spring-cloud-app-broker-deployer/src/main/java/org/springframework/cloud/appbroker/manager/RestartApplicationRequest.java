/*
 * Copyright 2002-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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

public class RestartApplicationRequest {

	private final String name;

	private final Map<String, String> properties;

	protected RestartApplicationRequest(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static RestartApplicationRequestBuilder builder() {
		return new RestartApplicationRequestBuilder();
	}

	public static final class RestartApplicationRequestBuilder {

		private String name;

		private final Map<String, String> properties = new HashMap<>();

		private RestartApplicationRequestBuilder() {
		}

		public RestartApplicationRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public RestartApplicationRequestBuilder properties(Map<String, String> properties) {
			if (!CollectionUtils.isEmpty(properties)) {
				this.properties.putAll(properties);
			}
			return this;
		}

		public RestartApplicationRequest build() {
			return new RestartApplicationRequest(name, properties);
		}

	}

}
