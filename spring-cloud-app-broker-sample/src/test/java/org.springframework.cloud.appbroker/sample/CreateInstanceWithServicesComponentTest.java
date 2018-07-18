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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.sample.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.sample.CreateInstanceWithServicesComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.apps[0].services[0]=my-db-service",
	"spring.cloud.appbroker.apps[0].services[1]=my-rabbit-service"
})
class CreateInstanceWithServicesComponentTest extends WiremockComponentTest {
	static final String APP_NAME = "appwithservices";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Test
	void shouldPushAppWithServiceWhenCreateServiceEndpointCalled() {
		// given that a service exists
		// TODO automate creation or stub bindings
		// cf cs p.mysql db-small my-db-service
		// cf cs p-rabbitmq standard my-rabbit-service

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.CREATED.value());

		// then a backing application is deployed
		String appsUrl = given()
			.header(getAuthorizationHeader())
			.get(baseCfUrl + "/v2/spaces/{spaceId}/apps?q=name:" + APP_NAME + "&page=1", SPACE_ID)
			.then()
			.body("resources[0].entity.name", is(equalToIgnoringWhiteSpace(APP_NAME)))
			.statusCode(HttpStatus.OK.value())
			.extract().body().jsonPath().getString("resources[0].metadata.url");

		// and the backing application has the services bound to it
		given()
			.header(getAuthorizationHeader())
			.get(baseCfUrl + appsUrl + "/env")
			.then()
			.body("system_env_json.VCAP_SERVICES.'p.mysql'[0].name",
				is(equalToIgnoringWhiteSpace("my-db-service")))
			.body("system_env_json.VCAP_SERVICES.'p-rabbitmq'[0].name",
				is(equalToIgnoringWhiteSpace("my-rabbit-service")))
			.statusCode(HttpStatus.OK.value());
	}

}