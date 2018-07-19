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
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

@TestComponent
public class CloudFoundryApiFixture {
	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-host}")
	private String cfApiHost;

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-port}")
	private int cfApiPort;

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.default-org}")
	private String cfDefaultOrg;

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.default-space}")
	private String cfDefaultSpace;

	@Value("${wiremock.cloudfoundry.access-token:an.access.token}")
	private String accessToken;

	private String defaultSpaceGuid;

	public void init() {
		String orgGuid = given(request())
			.when()
			.get("/v2/organizations?q=name:{orgName}", cfDefaultOrg)
			.then()
			.statusCode(HttpStatus.OK.value())
			.extract().body().jsonPath().getString("resources[0].metadata.guid");

		defaultSpaceGuid = given(request())
			.when()
			.get("/v2/spaces?q=name:{spaceName}&q=organization_guid:{orgGuid}", cfDefaultSpace, orgGuid)
			.then()
			.statusCode(HttpStatus.OK.value())
			.extract().body().jsonPath().getString("resources[0].metadata.guid");
	}

	public String findApplicationUrl(String appName) {
		return "/v2/spaces/" + defaultSpaceGuid + "/apps?q=name:" + appName + "&page=1";
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
