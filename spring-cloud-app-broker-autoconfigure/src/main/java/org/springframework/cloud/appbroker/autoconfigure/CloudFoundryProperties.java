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

package org.springframework.cloud.appbroker.autoconfigure;

import java.net.URI;

import org.cloudfoundry.reactor.ProxyConfiguration;

public class CloudFoundryProperties {

	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.cf";

	private String apiHost;

	private Integer apiPort;

	private String proxyHost;
	
	private int proxyPort;

	private String username;

	private String password;

	private String defaultOrg;

	private String defaultSpace;

	private boolean skipSslValidation;

	private int operationTimeoutSeconds = 60 * 60; // Timeout after 1 hour

	public String getApiHost() {
		return apiHost;
	}

	public void setApiHost(String apiHost) {
		this.apiHost = parseApiHost(apiHost);
	}

	public Integer getApiPort() {
		return apiPort;
	}

	public void setApiPort(int apiPort) {
		this.apiPort = apiPort;
	}

	private static String parseApiHost(String api) {
		final URI uri = URI.create(api);
		return uri.getHost() == null ? api : uri.getHost();
	}

	public ProxyConfiguration getProxyConfiguration() {
		return null;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
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
