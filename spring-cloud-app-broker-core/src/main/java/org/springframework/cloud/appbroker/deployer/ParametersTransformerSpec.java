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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class ParametersTransformerSpec {

	private String name;

	private Map<String, Object> args;

	private ParametersTransformerSpec() {
	}

	public ParametersTransformerSpec(String name, Map<String, Object> args) {
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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ParametersTransformerSpec that = (ParametersTransformerSpec) o;
		return Objects.equals(name, that.name) && Objects.equals(args, that.args);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, args);
	}

	public static ParametersTransformerSpecBuilder builder() {
		return new ParametersTransformerSpecBuilder();
	}

	public static final class ParametersTransformerSpecBuilder {

		private String name;

		private final Map<String, Object> args = new LinkedHashMap<>();

		private ParametersTransformerSpecBuilder() {
		}

		public ParametersTransformerSpecBuilder spec(ParametersTransformerSpec spec) {
			return this.name(spec.getName())
				.args(spec.getArgs());
		}

		public ParametersTransformerSpecBuilder name(String name) {
			this.name = name;
			return this;
		}

		public ParametersTransformerSpecBuilder arg(String key, Object value) {
			if (key != null && value != null) {
				this.args.put(key, value);
			}
			return this;
		}

		public ParametersTransformerSpecBuilder args(Map<String, Object> args) {
			if (!CollectionUtils.isEmpty(args)) {
				this.args.putAll(args);
			}
			return this;
		}

		public ParametersTransformerSpec build() {
			return new ParametersTransformerSpec(name, args);
		}

	}

}
