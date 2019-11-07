/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cloud.appbroker.integration.fixtures;

import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class UaaStubFixture extends WiremockStubFixture {

	protected UaaStubFixture() {
		super(8080);
	}

	public void stubCommonUaaRequests() {
		stubRetrieveAccessToken();
		stubRetrieveTokenKeys();
	}

	/**
	 * {
	 *  "jti": "9fd596e1fcd34c12a3f74695e8951b70",
	 *  "sub": "9f1a1425-a7ab-4e38-b2b9-d6f221b16cea",
	 *  "scope": [
	 *   "openid",
	 *   "routing.router_groups.write",
	 *   "network.write",
	 *   "scim.read",
	 *   "cloud_controller.admin",
	 *   "uaa.user",
	 *   "routing.router_groups.read",
	 *   "cloud_controller.read",
	 *   "password.write",
	 *   "cloud_controller.write",
	 *   "network.admin",
	 *   "doppler.firehose",
	 *   "scim.write"
	 *  ],
	 *  "client_id": "cf",
	 *  "cid": "cf",
	 *  "azp": "cf",
	 *  "grant_type": "password",
	 *  "user_id": "9f1a1425-a7ab-4e38-b2b9-d6f221b16cea",
	 *  "origin": "uaa",
	 *  "user_name": "admin",
	 *  "email": "admin",
	 *  "rev_sig": "c594512e",
	 *  "iat": 1539188141,
	 *  "exp": 1539195341,
	 *  "iss": "https://uaa.system.example.com/oauth/token",
	 *  "zid": "uaa",
	 *  "aud": [
	 *   "cloud_controller",
	 *   "scim",
	 *   "password",
	 *   "cf",
	 *   "uaa",
	 *   "openid",
	 *   "doppler",
	 *   "network",
	 *   "routing.router_groups"
	 *  ]
	 * }
	 */
	private void stubRetrieveAccessToken() {
		stubFor(post(urlPathEqualTo("/oauth/token"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(uaa("put-oauth-token"))));
	}

	private void stubRetrieveTokenKeys() {
		stubFor(get(urlPathEqualTo("/token_keys"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(uaa("get-token-keys"))));
	}

	public void stubCreateClient(String clientId) {
		stubFor(post(urlPathEqualTo("/oauth/clients"))
			.withRequestBody(matchingJsonPath("$.[?(@.client_id == '" + clientId + "')]"))
			.willReturn(ok()
				.withBody(uaa("post-oauth-clients",
					replace("@client-id", clientId)))));
	}

	public void stubDeleteClient(String clientId) {
		stubFor(delete(urlPathEqualTo("/oauth/clients/" + clientId))
			.willReturn(ok()
				.withBody(uaa("delete-oauth-clients"))));
	}

	private String uaa(String fileRoot, StringReplacementPair... replacements) {
		String response = readResponseFromFile(fileRoot, "uaa");
		for (StringReplacementPair pair : replacements) {
			response = response.replaceAll(pair.getRegex(), pair.getReplacement());
		}
		return response;
	}

}
