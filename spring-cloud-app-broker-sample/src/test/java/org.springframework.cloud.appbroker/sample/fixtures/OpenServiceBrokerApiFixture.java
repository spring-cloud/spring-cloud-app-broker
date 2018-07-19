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
import io.restassured.specification.RequestSpecification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.ApplicationListener;

import static io.restassured.RestAssured.with;

@TestComponent
public class OpenServiceBrokerApiFixture implements ApplicationListener<ApplicationStartedEvent> {
	private static final String ORG_ID = "org-id";
	private static final String SPACE_ID = "space-id";

	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	String planId;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	private String port;

	public String createServiceInstanceUrl() {
		return "/service_instances/{instance_id}";
	}

	public String deleteServiceInstanceUrl() {
		return "/service_instances/{instance_id}" +
			"?service_id=" + serviceDefinitionId +
			"&plan_id=" + planId;
	}

	public RequestSpecification serviceInstanceRequest() {
		return serviceBrokerSpecification()
			.body(buildServiceInstanceRequestBody());
	}

	private RequestSpecification serviceBrokerSpecification() {
		return with()
			.baseUri("http://localhost:" + port + "/v2")
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON);
	}

	private String buildServiceInstanceRequestBody() {
		return "{\n" +
			"  \"service_id\": \"" + serviceDefinitionId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"" + ORG_ID + "\",\n" +
			"  \"space_guid\": \"" + SPACE_ID + "\"\n" +
			"}\n";
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		this.port = event.getApplicationContext().getEnvironment().getProperty("local.server.port");
	}
}
