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

package org.springframework.cloud.appbroker.integration.fixtures;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class CredHubStubFixture extends WiremockStubFixture {
	protected CredHubStubFixture() {
		super(8888);
	}

	public void stubWriteCredential(String credentialName, ContentPattern<?>... appMetadataPatterns) {
		MappingBuilder mappingBuilder = put(urlPathEqualTo("/api/v1/data"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + credentialName + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.type == 'json')]"));
		for (ContentPattern<?> appMetadataPattern : appMetadataPatterns) {
			mappingBuilder.withRequestBody(appMetadataPattern);
		}
		stubFor(mappingBuilder
			.willReturn(ok()
				.withHeader("Content-type", "application/json")
				.withBody(credhub("put-data-json"))));
	}

	public void stubFindCredential(String credentialName) {
		stubFor(get(urlPathEqualTo("/api/v1/data"))
			.withQueryParam("name-like", equalTo(credentialName))
			.willReturn(ok()
				.withHeader("Content-type", "application/json")
				.withBody(credhub("get-data-find"))));
	}

	public void stubDeleteCredential(String credentialName) {
		stubFor(delete(urlPathEqualTo("/api/v1/data"))
			.withQueryParam("name", equalTo(credentialName))
			.willReturn(noContent()
				.withHeader("Content-type", "application/json")
				.withBody(credhub("put-data-json"))));
	}

	public void stubAddAppPermission(String path, String actor) {
		stubFor(post(urlPathEqualTo("/api/v2/permissions"))
			.withRequestBody(matchingJsonPath("$.[?(@.path == '" + path + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.actor == '" + actor + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.operations[0] == 'read')]"))
			.willReturn(ok()
				.withHeader("Content-type", "application/json")
				.withBody(credhub("post-permission"))));
	}

	public void stubGenerateUser(String appId, String serviceInstanceId,
								 String descriptor, int length) {
		stubGenerate(appId, serviceInstanceId, descriptor, length, "user", "post-data-user");
	}

	public void stubGeneratePassword(String appId, String serviceInstanceId,
									 String descriptor, int length) {
		stubGenerate(appId, serviceInstanceId, descriptor, length, "password", "post-data-password");
	}

	private void stubGenerate(String appId, String serviceInstanceId,
							  String descriptor, int length,
							  String type, String responseBody) {
		stubFor(post(urlPathEqualTo("/api/v1/data"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" +
				"/" + appId +
				"/" + serviceInstanceId +
				"/" + descriptor + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.type == '" + type + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.parameters.length == '" + length + "')]"))
			.willReturn(ok()
				.withHeader("Content-type", "application/json")
				.withBody(credhub(responseBody))));
	}

	public void stubDeleteCredential(String appId, String serviceInstanceId, String descriptor) {
		String credentialName = "/" + appId + "/" + serviceInstanceId + "/" + descriptor;
		stubFor(delete(urlPathEqualTo("/api/v1/data"))
			.withQueryParam("name", equalTo(credentialName))
			.willReturn(noContent()));
	}

	public String bindingCredentialName(String brokerAppName, String serviceDefinitionId,
										String bindingId, String credentialName) {
		return "/c/" + brokerAppName + "/" + serviceDefinitionId + "/" + bindingId + "/" + credentialName;
	}

	private String credhub(String fileRoot) {
		return readResponseFromFile(fileRoot, "credhub");
	}
}
