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

package org.springframework.cloud.appbroker.deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

public class GetApplicationResponse {

	private final String id;

	private final String name;

	private final Map<String, Object> environment;

	private final List<String> services;

	protected GetApplicationResponse(String id, String name, Map<String, Object> environment, List<String> services) {
		this.id = id;
		this.name = name;
		this.environment = environment;
		this.services = services;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getEnvironment() {
		return environment;
	}

	public List<String> getServices() {
		return services;
	}

	public static GetApplicationResponseBuilder builder() {
		return new GetApplicationResponseBuilder();
	}

	public static final class GetApplicationResponseBuilder {

		private String id;

		private String name;

		private final Map<String, Object> environment = new HashMap<>();

		private final List<String> services = new ArrayList<>();

		private GetApplicationResponseBuilder() {
		}

		public GetApplicationResponseBuilder id(String id) {
			this.id = id;
			return this;
		}

		public GetApplicationResponseBuilder name(String name) {
			this.name = name;
			return this;
		}

		public GetApplicationResponseBuilder environment(String key, String value) {
			if (key != null && value != null) {
				this.environment.put(key, value);
			}
			return this;
		}

		public GetApplicationResponseBuilder environment(Map<String, Object> environment) {
			if (!CollectionUtils.isEmpty(environment)) {
				this.environment.putAll(environment);
			}
			return this;
		}

		public GetApplicationResponseBuilder service(String service) {
			if (service != null) {
				this.services.add(service);
			}
			return this;
		}

		public GetApplicationResponseBuilder services(List<String> services) {
			if (!CollectionUtils.isEmpty(services)) {
				this.services.addAll(services);
			}
			return this;
		}

		public GetApplicationResponse build() {
			return new GetApplicationResponse(id, name, environment, services);
		}

	}

}
