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

package org.springframework.cloud.appbroker.oauth2;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class DeleteOAuth2ClientResponse {
	private final String clientId;
	private final String clientName;
	private final List<String> scopes;
	private final List<String> authorities;
	private final List<String> grantTypes;

	DeleteOAuth2ClientResponse(String clientId, String clientName,
							   List<String> scopes, List<String> authorities,
							   List<String> grantTypes) {
		this.clientId = clientId;
		this.clientName = clientName;
		this.scopes = scopes;
		this.authorities = authorities;
		this.grantTypes = grantTypes;
	}

	public String getClientId() {
		return clientId;
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

	public static DeleteOAuth2ClientResponseBuilder builder() {
		return new DeleteOAuth2ClientResponseBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DeleteOAuth2ClientResponse)) {
			return false;
		}
		DeleteOAuth2ClientResponse that = (DeleteOAuth2ClientResponse) o;
		return Objects.equals(clientId, that.clientId) &&
			Objects.equals(clientName, that.clientName) &&
			Objects.equals(scopes, that.scopes) &&
			Objects.equals(authorities, that.authorities) &&
			Objects.equals(grantTypes, that.grantTypes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(clientId, clientName, scopes, authorities, grantTypes);
	}

	@Override
	public String toString() {
		return "DeleteOAuth2ClientResponse{" +
			"clientId='" + clientId + '\'' +
			", clientName='" + clientName + '\'' +
			", scopes=" + scopes +
			", authorities=" + authorities +
			", grantTypes=" + grantTypes +
			'}';
	}

	public static class DeleteOAuth2ClientResponseBuilder {
		private String clientId;
		private String clientName;
		private final List<String> scopes = new ArrayList<>();
		private final List<String> authorities = new ArrayList<>();
		private final List<String> grantTypes = new ArrayList<>();

		DeleteOAuth2ClientResponseBuilder() {
		}

		public DeleteOAuth2ClientResponseBuilder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public DeleteOAuth2ClientResponseBuilder clientName(String name) {
			this.clientName = name;
			return this;
		}

		public DeleteOAuth2ClientResponseBuilder scopes(List<String> scopes) {
			if (!CollectionUtils.isEmpty(scopes)) {
				this.scopes.addAll(scopes);
			}
			return this;
		}

		public DeleteOAuth2ClientResponseBuilder authorities(List<String> authorities) {
			if (!CollectionUtils.isEmpty(authorities)) {
				this.authorities.addAll(authorities);
			}
			return this;
		}

		public DeleteOAuth2ClientResponseBuilder grantTypes(List<String> grantTypes) {
			if (!CollectionUtils.isEmpty(grantTypes)) {
				this.grantTypes.addAll(grantTypes);
			}
			return this;
		}

		public DeleteOAuth2ClientResponse build() {
			return new DeleteOAuth2ClientResponse(clientId, clientName, scopes, authorities, grantTypes);
		}
	}
}
