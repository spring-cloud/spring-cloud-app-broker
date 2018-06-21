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

package org.springframework.cloud.appbroker;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = CloudFoundryProperties.PROPERTY_PREFIX)
public class CloudFoundryProperties {

	static final String PROPERTY_PREFIX = "spring.cloud.app.broker.cf";

	private String api;

	private String username;

	private String password;

	private String defaultOrg;

	private String defaultSpace;

	private boolean skipSslValidation;

	private int operationTimeoutSeconds = 60 * 60; // Timeout after 1 hour

	public void setApi(String api) {
		this.api = getApiHost(api);
	}

	public static String getApiHost(String api) {
		final URI uri = URI.create(api);
		return uri.getHost() == null ? api : uri.getHost();
	}

	public String getApi() {
		return api;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDefaultOrg() {
		return defaultOrg;
	}

	public void setDefaultOrg(String defaultOrg) {
		this.defaultOrg = defaultOrg;
	}

	public String getDefaultSpace() {
		return defaultSpace;
	}

	public void setDefaultSpace(String defaultSpace) {
		this.defaultSpace = defaultSpace;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	public int getOperationTimeoutSeconds() {
		return operationTimeoutSeconds;
	}

	public void setOperationTimeoutSeconds(int operationTimeoutSeconds) {
		this.operationTimeoutSeconds = operationTimeoutSeconds;
	}


}
