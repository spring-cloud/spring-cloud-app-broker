/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.sample;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.apps[0].name=appwithservices",
	"spring.cloud.appbroker.apps[0].services[0]=my-db-service"
})
class CreateInstanceWithServicesComponentTest extends WiremockComponentTest {

	@Test
	void shouldPushAppWithServiceWhenCreateServiceEndpointCalled() {
		// given that a service exists
		// TODO automate creation or stub bindings
		// cf cs p.mysql db-small my-db-service

		// when the provision is called with the service property
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody())
			.put(baseUrl + "/v2/service_instances/{instance_id}", "instance-id")
			.then()
			.statusCode(HttpStatus.CREATED.value());

		// then an instance is created
		String appsUrl = given()
			.header(getAuthorizationHeader())
			.get(baseCfUrl + "/v2/spaces/{spaceId}/apps?q=name:appwithservices&page=1", SPACE_ID)
			.then()
			.body("resources[0].entity.name", is(equalToIgnoringWhiteSpace("appwithservices")))
			.statusCode(200)
			.extract().body().jsonPath().getString("resources[0].metadata.url");

		// and it has the service bind to it
		given()
			.header(getAuthorizationHeader())
			.get(baseCfUrl + appsUrl + "/env")
			.then()
			.body("system_env_json.VCAP_SERVICES.'p.mysql'[0].name",
				is(equalToIgnoringWhiteSpace("my-db-service")))
			.statusCode(200);
	}

}