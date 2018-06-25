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

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.appbroker.sample.CreateInstanceComponentTest.TestConfig.PROXY_PORT;
import static org.springframework.cloud.appbroker.sample.CreateInstanceComponentTest.TestConfig.SIMULATED_CF_HOST;

/**
 * This is a black box test to validate the end-to-end flow of deploying an application from a service broker request.
 * The black box test validates the flow has been performed and external contracts are satisfied but does not
 * rely on external dependencies (eg pushing an app to a real CF)
 * {@see https://github.com/spring-cloud-incubator/spring-cloud-app-broker/issues/4}
 */
@ExtendWith(SpringExtension.class)
//TODO: Fix Hoverfly replaying of CF responses via a proxy
//@ExtendWith(HoverflyExtension.class)
//@HoverflySimulate(source = @HoverflySimulate.Source(value = "requests.json", type = HoverflySimulate.SourceType.CLASSPATH),
//	config = @HoverflyConfig(proxyPort = PROXY_PORT, destination = SIMULATED_CF_HOST))
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {
		CreateInstanceComponentTest.TestConfig.class,
		AppBrokerSampleApplication.class
	},
	properties = {
		"spring.cloud.appbroker.cf.apiHost=" + SIMULATED_CF_HOST,
		"spring.cloud.appbroker.cf.apiPort=80",
		"spring.cloud.appbroker.cf.proxyHost=http://localhost",
		"spring.cloud.appbroker.cf.proxyPort=" + PROXY_PORT,
	}
)
class CreateInstanceComponentTest {

	private String baseUrl = "";

	@Value("${local.server.port}")
	private String port;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	private String serviceDefinitionId;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	private String planId;

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port;
	}

	/**
	 * TODO: This is a placeholder test to verify that the app context is initialized properly for test cases
	 * in this class to run. It is only testing a code path through the Spring Cloud Open Service Broker
	 * project, so it likely has no long-term value here.
	 */
	@Test
	void shouldFailWithInvalidServiceDefinitionId() {
		final String instanceId = "123";
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody("invalid service definition id", planId))
			.put(baseUrl + "/v2/service_instances/{instance_id}", instanceId)
			.then()
			.statusCode(HttpStatus.UNPROCESSABLE_ENTITY.value());
	}

	@Disabled
	@Test
	void shouldPushAppWhenCreateServiceEndpointCalled() {
		final String instanceId = "123";
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody(serviceDefinitionId, planId))
			.put(baseUrl + "/v2/service_instances/{instance_id}", instanceId)
			.then()
			.statusCode(HttpStatus.CREATED.value());


		// deployer app object
		// TODO assert cloudfoundry API contract for pushing our helloworld application was satisfied
		assertThat("The CF API endpoint was called with operation").isEqualTo("push");// which is the value from the deployer
	}

	private static String createDefaultBody(String serviceDefinitionId, String planId) {
		return "{\n" +
			"  \"service_id\": \"" + serviceDefinitionId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\"\n" +
			"}\n";
	}

	@Configuration
	static class TestConfig {
		final static int PROXY_PORT = 8500;
		final static String SIMULATED_CF_HOST = "http://api.cf.fakecf.com";
	}
}
