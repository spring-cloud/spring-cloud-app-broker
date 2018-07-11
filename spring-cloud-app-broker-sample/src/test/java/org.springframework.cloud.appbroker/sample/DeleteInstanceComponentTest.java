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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class DeleteInstanceComponentTest extends WiremockComponentTest {

	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	private String planId;
	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	private String serviceDefinitionId;

	@Test
	void shouldDeleteAppsWhenDeleteServiceEndpointCalled() {
		// given an instance is created
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody())
			.put(baseUrl + "/v2/service_instances/{instance_id}", "instance-id")
			.then()
			.statusCode(HttpStatus.CREATED.value());

		// when the deprovision is called
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody())
			.delete(baseUrl + "/v2/service_instances/{instance_id}?service_id=" + serviceDefinitionId +
				"&plan_id=" + planId, "instance-id")
			.then()
			.statusCode(HttpStatus.OK.value());

		// then the instance is deleted
		given()
			.header(getAuthorizationHeader())
			.get(baseCfUrl + "/v2/spaces/{spaceId}/apps?q=name:helloworldapp&page=1", getSpaceId())
			.then()
			.body("resources.size", is(equalTo(0)))
			.statusCode(200);
	}

	private String createDefaultBody() {
		return "{\n" +
			"  \"service_id\": \"" + serviceDefinitionId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"org-guid-here\",\n" +
			"  \"space_guid\": \"" + getSpaceId() + "\"\n" +
			"}\n";
	}

}