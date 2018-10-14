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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.net.URI;

import org.cloudfoundry.reactor.ProxyConfiguration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import static org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryProperties.PROPERTY_PREFIX;

@ConfigurationProperties(PROPERTY_PREFIX)
public class CloudFoundryProperties {

	static final String PROPERTY_PREFIX = "spring.cloud.appbroker.acceptancetest.cloudfoundry";

	private String apiHost;
	private Integer apiPort;
	private String defaultOrg;
	private String defaultSpace;
	private String username;
	private String password;
	private String clientId;
	private String clientSecret;
	private String identityZoneSubdomain;
	private boolean secure = true;
	private boolean skipSslValidation;

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

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public String getIdentityZoneSubdomain() {
		return identityZoneSubdomain;
	}

	public void setIdentityZoneSubdomain(String identityZoneSubdomain) {
		this.identityZoneSubdomain = identityZoneSubdomain;
	}

	public ProxyConfiguration getProxyConfiguration() {
		return null;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isSkipSslValidation() {
		return skipSslValidation;
	}

	public void setSkipSslValidation(boolean skipSslValidation) {
		this.skipSslValidation = skipSslValidation;
	}

	private static String parseApiHost(String api) {
		final URI uri = URI.create(api);
		return uri.getHost() == null ? api : uri.getHost();
	}

}
