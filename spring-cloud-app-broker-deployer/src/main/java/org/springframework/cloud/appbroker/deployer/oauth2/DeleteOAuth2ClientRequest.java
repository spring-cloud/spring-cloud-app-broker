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

import java.util.Objects;

public class DeleteOAuth2ClientRequest {

	private final String clientId;

	private final String identityZoneSubdomain;

	private final String identityZoneId;

	protected DeleteOAuth2ClientRequest(String clientId, String identityZoneSubdomain, String identityZoneId) {
		this.clientId = clientId;
		this.identityZoneSubdomain = identityZoneSubdomain;
		this.identityZoneId = identityZoneId;
	}

	public String getClientId() {
		return this.clientId;
	}

	public String getIdentityZoneSubdomain() {
		return this.identityZoneSubdomain;
	}

	public String getIdentityZoneId() {
		return this.identityZoneId;
	}

	public static DeleteOAuth2ClientRequestBuilder builder() {
		return new DeleteOAuth2ClientRequestBuilder();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof DeleteOAuth2ClientRequest)) {
			return false;
		}
		DeleteOAuth2ClientRequest that = (DeleteOAuth2ClientRequest) o;
		return Objects.equals(this.clientId, that.clientId)
				&& Objects.equals(this.identityZoneSubdomain, that.identityZoneSubdomain)
				&& Objects.equals(this.identityZoneId, that.identityZoneId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.clientId, this.identityZoneSubdomain, this.identityZoneId);
	}

	@Override
	public String toString() {
		return "DeleteOAuth2ClientRequest{" + "clientId='" + this.clientId + '\'' + ", identityZoneSubdomain='"
				+ this.identityZoneSubdomain + '\'' + ", identityZoneId='" + this.identityZoneId + '\'' + '}';
	}

	public static final class DeleteOAuth2ClientRequestBuilder {

		private String clientId;

		private String identityZoneSubdomain;

		private String identityZoneId;

		private DeleteOAuth2ClientRequestBuilder() {
		}

		public DeleteOAuth2ClientRequestBuilder clientId(String clientId) {
			this.clientId = clientId;
			return this;
		}

		public DeleteOAuth2ClientRequestBuilder identityZoneSubdomain(String identityZoneSubdomain) {
			this.identityZoneSubdomain = identityZoneSubdomain;
			return this;
		}

		public DeleteOAuth2ClientRequestBuilder identityZoneId(String identityZoneId) {
			this.identityZoneId = identityZoneId;
			return this;
		}

		public DeleteOAuth2ClientRequest build() {
			return new DeleteOAuth2ClientRequest(this.clientId, this.identityZoneSubdomain, this.identityZoneId);
		}

	}

}
