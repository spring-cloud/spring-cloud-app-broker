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

package org.springframework.cloud.appbroker.deployer;

import java.util.Map;

public class CreateServiceKeyResponse {

	private final String name;
	private final Map<String, Object> credentials;

	public CreateServiceKeyResponse(String name, Map<String, Object> credentials) {
		this.name = name;
		this.credentials = credentials;
	}

	public static CreateServiceKeyResponseBuilder builder() {
		return new CreateServiceKeyResponseBuilder();
	}

	public String getName() {
		return name;
	}

	public Map<String, Object> getCredentials() {
		return credentials;
	}

	public static final class CreateServiceKeyResponseBuilder {

		private String name;
		private Map<String, Object> credentials;

		private CreateServiceKeyResponseBuilder() {
		}

		public CreateServiceKeyResponseBuilder name(String name) {
			this.name = name;
			return this;
		}

		public CreateServiceKeyResponseBuilder credentials(Map<String, Object> credentials) {
			this.credentials = credentials;
			return this;
		}

		public CreateServiceKeyResponse build() {
			return new CreateServiceKeyResponse(name, credentials);
		}
	}
}
