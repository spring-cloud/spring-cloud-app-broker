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

package org.springframework.cloud.appbroker.oauth2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CreateOAuth2ClientRequest {

	private final String clientId;
	private final String clientSecret;
	private final String clientName;
	private final List<String> scopes;
	private final List<String> authorities;
	private final List<String> grantTypes;
	private final String identityZoneSubdomain;
	private final String identityZoneId;

	CreateOAuth2ClientRequest(String clientId, String clientSecret, String clientName,
							  List<String> scopes, List<String> authorities, List<String> grantTypes,
							  String identityZoneSubdomain, String identityZoneId) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.clientName = clientName;
		this.scopes = scopes;
		this.authorities = authorities;
		this.grantTypes = grantTypes;
		this.identityZoneSubdomain = identityZoneSubdomain;
		this.identityZoneId = identityZoneId;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public String getClientName() {
		return clientName;
	}

	public List<String> getScopes() {
		return scopes;
	}

	public List<String> getAuthorities() {
		return authorities;
	}

	public List<String> getGrantTypes() {
		return grantTypes;
	}

	public String getIdentityZoneSubdomain() {
		return identityZoneSubdomain;
	}

	public String getIdentityZoneId() {
		return identityZoneId;
	}

	public static CreateOAuth2ClientRequestBuilder builder() {
		return new CreateOAuth2ClientRequestBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CreateOAuth2ClientRequest)) {
			return false;
		}
		CreateOAuth2ClientRequest that = (CreateOAuth2ClientRequest) o;
		return Objects.equals(clientId, that.clientId) &&
			Objects.equals(clientSecret, that.clientSecret) &&
			Objects.equals(clientName, that.clientName) &&
			Objects.equals(scopes, that.scopes) &&
			Objects.equals(authorities, that.authorities) &&
			Objects.equals(grantTypes, that.grantTypes) &&
			Objects.equals(identityZoneSubdomain, that.identityZoneSubdomain) &&
			Objects.equals(identityZoneId, that.identityZoneId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientId, clientSecret, clientName, scopes, authorities,
			grantTypes, identityZoneSubdomain, identityZoneId);
	}

	@Override
	public String toString() {
		return "CreateOAuth2ClientRequest{" +
			"clientId='" + clientId + '\'' +
			", clientSecret='" + clientSecret + '\'' +
			", clientName='" + clientName + '\'' +
			", scopes=" + scopes +
			", authorities=" + authorities +
			", grantTypes=" + grantTypes +
			", identityZoneSubdomain='" + identityZoneSubdomain + '\'' +
			", identityZoneId='" + identityZoneId + '\'' +
			'}';
	}

	public static class CreateOAuth2ClientRequestBuilder {

		private String clientId;
		private String clientSecret;
		private String clientName;
		private List<String> scopes;
		private List<String> authorities;
		private List<String> grantTypes;
		private String identityZoneSubdomain;
		private String identityZoneId;

		CreateOAuth2ClientRequestBuilder() {
		}

		public CreateOAuth2ClientRequestBuilder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public CreateOAuth2ClientRequestBuilder clientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
			return this;
		}

		public CreateOAuth2ClientRequestBuilder clientName(String clientName) {
			this.clientName = clientName;
			return this;
		}

		public CreateOAuth2ClientRequestBuilder scopes(String... scopes) {
			this.scopes = Arrays.asList(scopes);
			return this;
		}

		public CreateOAuth2ClientRequestBuilder authorities(String... authorities) {
			this.authorities = Arrays.asList(authorities);
			return this;
		}

		public CreateOAuth2ClientRequestBuilder grantTypes(String... grantTypes) {
			this.grantTypes = Arrays.asList(grantTypes);
			return this;
		}

		public CreateOAuth2ClientRequestBuilder identityZoneSubdomain(String identityZoneSubdomain) {
			this.identityZoneSubdomain = identityZoneSubdomain;
			return this;
		}

		public CreateOAuth2ClientRequestBuilder identityZoneId(String identityZoneId) {
			this.identityZoneId = identityZoneId;
			return this;
		}

		public CreateOAuth2ClientRequest build() {
			return new CreateOAuth2ClientRequest(clientId, clientSecret, clientName,
				scopes, authorities, grantTypes,
				identityZoneSubdomain, identityZoneId);
		}
	}
}
