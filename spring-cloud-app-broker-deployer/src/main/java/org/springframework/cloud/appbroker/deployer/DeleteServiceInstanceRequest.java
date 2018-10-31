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

public class DeleteServiceInstanceRequest {

	private final String name;

	DeleteServiceInstanceRequest(String name) {
		this.name = name;
	}

	public static DeleteServiceInstanceRequestBuilder builder() {
		return new DeleteServiceInstanceRequestBuilder();
	}

	public String getName() {
		return name;
	}

	public static class DeleteServiceInstanceRequestBuilder {

		private String name;

		DeleteServiceInstanceRequestBuilder() {
		}

		public DeleteServiceInstanceRequestBuilder name(String name) {
			this.name = name;
			return this;
		}

		public DeleteServiceInstanceRequest build() {
			return new DeleteServiceInstanceRequest(name);
		}

	}
}
