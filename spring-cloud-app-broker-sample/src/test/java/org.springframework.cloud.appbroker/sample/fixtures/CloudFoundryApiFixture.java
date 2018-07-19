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

import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import static io.restassured.RestAssured.with;

@TestComponent
public class CloudFoundryApiFixture {
	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-host}")
	private String cfApiHost;

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-port}")
	private int cfApiPort;

	@Value("${wiremock.cloudfoundry.access-token:an.access.token}")
	private String accessToken;

	public String getApplicationUrl(String spaceGuid, String appName) {
		return "/v2/spaces/" + spaceGuid + "/apps?q=name:" + appName + "&page=1";
	}

	public RequestSpecification request() {
		return with()
			.baseUri("http://" + cfApiHost + ":" + cfApiPort)
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON)
			.header(getAuthorizationHeader());
	}

	private Header getAuthorizationHeader() {
		return new Header("Authorization", "bearer " + accessToken);
	}
}
