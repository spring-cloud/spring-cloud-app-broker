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

package org.springframework.cloud.appbroker.sample;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.sample.fixtures.CloudFoundryApiFixture;
import org.springframework.cloud.appbroker.sample.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.sample.CreateInstanceWithEnvironmentComponentTest.APP_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.apps[0].environment.ENV_VAR_1=value1",
	"spring.cloud.appbroker.apps[0].environment.ENV_VAR_2=true",
})
class CreateInstanceWithEnvironmentComponentTest extends WiremockComponentTest {
	static final String APP_NAME = "app-with-env";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudFoundryApiFixture cloudFoundryFixture;

	@Test
	void shouldPushAppWithEnvironmentWhenCreateServiceEndpointCalled() {
		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.CREATED.value());

		// then a backing application is deployed
		String appsUrl = given(cloudFoundryFixture.request())
			.when()
			.get(cloudFoundryFixture.findApplicationUrl(APP_NAME))
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("resources[0].entity.name", is(equalToIgnoringWhiteSpace(APP_NAME)))
			.extract().body().jsonPath().getString("resources[0].metadata.url");

		// and the backing application has the services bound to it
		String springAppJson = given(cloudFoundryFixture.request())
			.when()
			.get(appsUrl + "/env")
			.then()
			.statusCode(HttpStatus.OK.value())
			.extract().body().jsonPath().getString("environment_json.SPRING_APPLICATION_JSON");

		assertThat(JsonPath.given(springAppJson).getString("ENV_VAR_1")).isEqualTo("value1");
		assertThat(JsonPath.given(springAppJson).getBoolean("ENV_VAR_2")).isEqualTo(true);
	}

}