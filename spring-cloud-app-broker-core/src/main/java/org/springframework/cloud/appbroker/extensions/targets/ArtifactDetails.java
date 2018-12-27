/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.HashMap;
import java.util.Map;

public class ArtifactDetails {

	private final String name;
	private final Map<String, String> properties;

	ArtifactDetails(String name, Map<String, String> properties) {
		this.name = name;
		this.properties = properties;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public static ArtifactDetailsBuilder builder() {
		return new ArtifactDetailsBuilder();
	}

	public static final class ArtifactDetailsBuilder {

		private String name;
		private final Map<String, String> properties = new HashMap<>();

		ArtifactDetailsBuilder() {
		}

		public ArtifactDetailsBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ArtifactDetailsBuilder properties(Map<String, String> properties) {
			if (properties == null) {
				return this;
			}
			this.properties.putAll(properties);
			return this;
		}

		public ArtifactDetails build() {
			return new ArtifactDetails(name, properties);
		}

	}

}
