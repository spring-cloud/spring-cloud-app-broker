/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.util.CollectionUtils;

public class CredentialProviderSpec {

	private String name;
	private Map<String, Object> args;

	private CredentialProviderSpec() {
	}

	CredentialProviderSpec(String name, Map<String, Object> args) {
		this.name = name;
		this.args = args;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getArgs() {
		return args;
	}

	public void setArgs(Map<String, Object> args) {
		this.args = args;
	}

	public static CredentialProviderSpecBuilder builder() {
		return new CredentialProviderSpecBuilder();
	}

	public static class CredentialProviderSpecBuilder {

		private String name;

		private final Map<String, Object> args = new LinkedHashMap<>();

		CredentialProviderSpecBuilder() {
		}

		public CredentialProviderSpecBuilder spec(CredentialProviderSpec spec) {
			return this.name(spec.getName())
				.args(spec.getArgs());
		}

		public CredentialProviderSpecBuilder name(String name) {
			this.name = name;
			return this;
		}

		public CredentialProviderSpecBuilder arg(String key, Object value) {
			if (key != null && value != null) {
				this.args.put(key, value);
			}
			return this;
		}

		public CredentialProviderSpecBuilder args(Map<String, Object> args) {
			if (!CollectionUtils.isEmpty(args)) {
				this.args.putAll(args);
			}
			return this;
		}

		public CredentialProviderSpec build() {
			return new CredentialProviderSpec(name, args);
		}
	}
}
