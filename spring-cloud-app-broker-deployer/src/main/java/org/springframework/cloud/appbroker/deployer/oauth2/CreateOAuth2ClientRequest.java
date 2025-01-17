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

package org.springframework.cloud.appbroker.deployer.oauth2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class CreateOAuth2ClientRequest {

	private final String clientId;

	private final String clientSecret;

	private final String clientName;

	private final List<String> scopes;

	private final List<String> authorities;

	private final List<String> grantTypes;

	private final String identityZoneSubdomain;

	private final String identityZoneId;

	protected CreateOAuth2ClientRequest(String clientId, String clientSecret, String clientName, List<String> scopes,
			List<String> authorities, List<String> grantTypes, String identityZoneSubdomain, String identityZoneId) {
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
		return this.clientId;
	}

	public String getClientSecret() {
		return this.clientSecret;
	}

	public String getClientName() {
		return this.clientName;
	}

	public List<String> getScopes() {
		return this.scopes;
	}

	public List<String> getAuthorities() {
		return this.authorities;
	}

	public List<String> getGrantTypes() {
		return this.grantTypes;
	}

	public String getIdentityZoneSubdomain() {
		return this.identityZoneSubdomain;
	}

	public String getIdentityZoneId() {
		return this.identityZoneId;
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
		return Objects.equals(this.clientId, that.clientId) && Objects.equals(this.clientSecret, that.clientSecret)
				&& Objects.equals(this.clientName, that.clientName) && Objects.equals(this.scopes, that.scopes)
				&& Objects.equals(this.authorities, that.authorities)
				&& Objects.equals(this.grantTypes, that.grantTypes)
				&& Objects.equals(this.identityZoneSubdomain, that.identityZoneSubdomain)
				&& Objects.equals(this.identityZoneId, that.identityZoneId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.clientId, this.clientSecret, this.clientName, this.scopes, this.authorities,
				this.grantTypes, this.identityZoneSubdomain, this.identityZoneId);
	}

	@Override
	public String toString() {
		return "CreateOAuth2ClientRequest{" + "clientId='" + this.clientId + '\'' + ", clientSecret='"
				+ this.clientSecret + '\'' + ", clientName='" + this.clientName + '\'' + ", scopes=" + this.scopes
				+ ", authorities=" + this.authorities + ", grantTypes=" + this.grantTypes + ", identityZoneSubdomain='"
				+ this.identityZoneSubdomain + '\'' + ", identityZoneId='" + this.identityZoneId + '\'' + '}';
	}

	public static final class CreateOAuth2ClientRequestBuilder {

		private String clientId;

		private String clientSecret;

		private String clientName;

		private final List<String> scopes = new ArrayList<>();

		private final List<String> authorities = new ArrayList<>();

		private final List<String> grantTypes = new ArrayList<>();

		private String identityZoneSubdomain;

		private String identityZoneId;

		private CreateOAuth2ClientRequestBuilder() {
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

		public CreateOAuth2ClientRequestBuilder scopes(List<String> scopes) {
			if (!CollectionUtils.isEmpty(scopes)) {
				this.scopes.addAll(scopes);
			}
			return this;
		}

		public CreateOAuth2ClientRequestBuilder scopes(String... scopes) {
			this.scopes(Arrays.asList(scopes));
			return this;
		}

		public CreateOAuth2ClientRequestBuilder authorities(List<String> authorities) {
			if (!CollectionUtils.isEmpty(authorities)) {
				this.authorities.addAll(authorities);
			}
			return this;
		}

		public CreateOAuth2ClientRequestBuilder authorities(String... authorities) {
			this.authorities(Arrays.asList(authorities));
			return this;
		}

		public CreateOAuth2ClientRequestBuilder grantTypes(List<String> grantTypes) {
			if (!CollectionUtils.isEmpty(grantTypes)) {
				this.grantTypes.addAll(grantTypes);
			}
			return this;
		}

		public CreateOAuth2ClientRequestBuilder grantTypes(String... grantTypes) {
			this.grantTypes(Arrays.asList(grantTypes));
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
			return new CreateOAuth2ClientRequest(this.clientId, this.clientSecret, this.clientName, this.scopes,
					this.authorities, this.grantTypes, this.identityZoneSubdomain, this.identityZoneId);
		}

	}

}
