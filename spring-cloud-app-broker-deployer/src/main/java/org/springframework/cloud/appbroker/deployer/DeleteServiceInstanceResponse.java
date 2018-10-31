/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

public class DeleteServiceInstanceResponse {

	private final String name;

	DeleteServiceInstanceResponse(String name) {
		this.name = name;
	}

	public static DeleteServiceInstanceResponseBuilder builder() {
		return new DeleteServiceInstanceResponseBuilder();
	}

	public String getName() {
		return name;
	}

	public static class DeleteServiceInstanceResponseBuilder {

		private String name;

		DeleteServiceInstanceResponseBuilder() {
		}

		public DeleteServiceInstanceResponseBuilder name(String name) {
			this.name = name;
			return this;
		}

		public DeleteServiceInstanceResponse build() {
			return new DeleteServiceInstanceResponse(name);
		}

	}
}
