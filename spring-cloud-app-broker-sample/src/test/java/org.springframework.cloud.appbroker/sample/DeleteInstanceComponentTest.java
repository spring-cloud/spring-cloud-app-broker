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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.sample.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.sample.DeleteInstanceComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.apps[0].name=" + APP_NAME,
})
class DeleteInstanceComponentTest extends WiremockComponentTest {
	static final String APP_NAME = "helloworldapp";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Test
	void shouldDeleteAppsWhenDeleteServiceEndpointCalled() {
		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.CREATED.value());

		// when the service instance is deleted
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.delete(brokerFixture.deleteServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.OK.value());

		// then the backing application is deleted
		given()
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON)
			.header(getAuthorizationHeader())
			.get(baseCfUrl + "/v2/spaces/{spaceId}/apps?q=name:" + APP_NAME + "&page=1", SPACE_ID)
			.then()
			.contentType(ContentType.JSON)
			.body("resources.size", is(equalTo(0)))
			.statusCode(HttpStatus.OK.value());
	}

}