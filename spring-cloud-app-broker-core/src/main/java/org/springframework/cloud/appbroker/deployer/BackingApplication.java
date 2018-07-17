/*
 * Copyright 2016-2018 the original author or authors.
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

import java.util.List;
import java.util.Map;

public class BackingApplication {

	private String name;
	private String path;
	private Map<String, String> properties;
	private List<String> services;

	BackingApplication() {
	}

	public BackingApplication(String name, String path) {
		this(name, path, null, null);
	}

	BackingApplication(String name, String path, Map<String, String> properties) {
		this(name, path, properties, null);
	}

	BackingApplication(String name, String path, List<String> services) {
		this(name, path, null, services);
	}

	private BackingApplication(String name, String path, Map<String, String> properties, List<String> services) {
		this.name = name;
		this.path = path;
		this.properties = properties;
		this.services = services;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public List<String> getServices() {
		return services;
	}

	public void setServices(List<String> services) {
		this.services = services;
	}
}
