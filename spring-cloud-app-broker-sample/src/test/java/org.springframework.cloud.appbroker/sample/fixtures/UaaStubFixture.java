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

package org.springframework.cloud.appbroker.sample.fixtures;

import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class UaaStubFixture extends WiremockStubFixture {
	public void stubCommonUaaRequests() {
		stubRetrieveAccessToken();
	}

	private void stubRetrieveAccessToken() {
		stubFor(post(urlPathEqualTo("/oauth/token"))
			.willReturn(ok()
				.withBody(uaa("put-oauth-token"))));
	}

	private String uaa(String fileRoot) {
		return readResponseFromFile(fileRoot, "uaa");
	}
}
