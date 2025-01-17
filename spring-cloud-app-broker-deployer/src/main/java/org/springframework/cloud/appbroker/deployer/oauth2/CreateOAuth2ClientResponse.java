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
import java.util.List;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class CreateOAuth2ClientResponse {

	private final String clientId;

	private final String clientName;

	private final List<String> scopes;

	private final List<String> authorities;

	private final List<String> grantTypes;

	protected CreateOAuth2ClientResponse(String clientId, String clientName, List<String> scopes,
			List<String> authorities, List<String> grantTypes) {

		this.clientId = clientId;
		this.clientName = clientName;
		this.scopes = scopes;
		this.authorities = authorities;
		this.grantTypes = grantTypes;
	}

	public String getClientId() {
		return this.clientId;
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

	public static CreateOAuth2ClientResponseBuilder builder() {
		return new CreateOAuth2ClientResponseBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CreateOAuth2ClientResponse)) {
			return false;
		}
		CreateOAuth2ClientResponse that = (CreateOAuth2ClientResponse) o;
		return Objects.equals(this.clientId, that.clientId) && Objects.equals(this.clientName, that.clientName)
				&& Objects.equals(this.scopes, that.scopes) && Objects.equals(this.authorities, that.authorities)
				&& Objects.equals(this.grantTypes, that.grantTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.clientId, this.clientName, this.scopes, this.authorities, this.grantTypes);
	}

	@Override
	public String toString() {
		return "CreateOAuth2ClientResponse{" + "clientId='" + this.clientId + '\'' + ", clientName='" + this.clientName
				+ '\'' + ", scopes=" + this.scopes + ", authorities=" + this.authorities + ", grantTypes="
				+ this.grantTypes + '}';
	}

	public static final class CreateOAuth2ClientResponseBuilder {

		private String clientId;

		private String clientName;

		private final List<String> scopes = new ArrayList<>();

		private final List<String> authorities = new ArrayList<>();

		private final List<String> grantTypes = new ArrayList<>();

		private CreateOAuth2ClientResponseBuilder() {
		}

		public CreateOAuth2ClientResponseBuilder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder clientName(String name) {
			this.clientName = name;
			return this;
		}

		public CreateOAuth2ClientResponseBuilder scopes(List<String> scopes) {
			if (scopes != null) {
				this.scopes.addAll(scopes);
			}
			return this;
		}

		public CreateOAuth2ClientResponseBuilder authorities(List<String> authorities) {
			if (!CollectionUtils.isEmpty(authorities)) {
				this.authorities.addAll(authorities);
			}
			return this;
		}

		public CreateOAuth2ClientResponseBuilder grantTypes(List<String> grantTypes) {
			if (!CollectionUtils.isEmpty(grantTypes)) {
				this.grantTypes.addAll(grantTypes);
			}
			return this;
		}

		public CreateOAuth2ClientResponse build() {
			return new CreateOAuth2ClientResponse(this.clientId, this.clientName, this.scopes, this.authorities,
					this.grantTypes);
		}

	}

}
