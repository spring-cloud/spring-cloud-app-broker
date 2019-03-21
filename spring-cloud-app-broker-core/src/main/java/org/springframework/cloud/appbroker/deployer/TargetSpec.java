/*
 * Copyright 2016-2018. the original author or authors.
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

public class TargetSpec {

	private String name;

	private TargetSpec() {
	}

	TargetSpec(String name) {
		this.name = name;
	}

	TargetSpec(TargetSpec targetSpecToClone) {
		this.name = targetSpecToClone.name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public static TargetSpecBuilder builder() {
		return new TargetSpecBuilder();
	}

	public static class TargetSpecBuilder {

		private String name;

		TargetSpecBuilder() {
		}

		public TargetSpecBuilder name(String name) {
			this.name = name;
			return this;
		}

		public TargetSpec build() {
			return new TargetSpec(name);
		}
	}
}
