/*
 * Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.appbroker.sample;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import io.restassured.http.ContentType;
import io.specto.hoverfly.junit.core.Hoverfly;
import io.specto.hoverfly.junit.core.HoverflyConfig;
import io.specto.hoverfly.junit.core.HoverflyMode;
import io.specto.hoverfly.junit.core.config.LocalHoverflyConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.cloud.appbroker.sample.CreateInstanceComponentTest.SIMULATED_CF_HOST;
import static org.springframework.cloud.appbroker.sample.CreateInstanceComponentTest.SIMULATED_CF_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {AppBrokerSampleApplication.class},
	properties = {
		"spring.cloud.appbroker.deploy.path=classpath:demo.jar",
		"spring.cloud.appbroker.deploy.appName=helloworldapp",
		"spring.cloud.appbroker.cf.apiHost=" + SIMULATED_CF_HOST,
		"spring.cloud.appbroker.cf.apiPort=" + SIMULATED_CF_PORT,
		"spring.cloud.appbroker.cf.username=admin",
		"spring.cloud.appbroker.cf.password=adminpass",
		"spring.cloud.appbroker.cf.defaultOrg=test",
		"spring.cloud.appbroker.cf.defaultSpace=development",
		"spring.cloud.appbroker.cf.secure=false"

	})
@TestPropertySource({
	"classpath:application-openservicebroker-catalog.yml",
})
class CreateInstanceComponentTest {

	final static String SIMULATED_CF_HOST = "http://localhost";
	final static int SIMULATED_CF_PORT = 8500;

	private static Hoverfly hoverfly;
	private String baseCfUrl;
	private String baseUrl = "";
	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	private String planId;
	@Value("${local.server.port}")
	private String port;
	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	private String serviceDefinitionId;

	@BeforeEach
	void setUp() {
		String localhost = "http://localhost";
		baseUrl = localhost + ":" + port;
		baseCfUrl = localhost + ":" + SIMULATED_CF_PORT;
	}

	@BeforeAll
	static void setUpClass() throws IOException {
		setupHoverfly();
	}

	@AfterAll
	static void tearDownClass() {
		hoverfly.close();
	}

	@Test
	void shouldPushAppWhenCreateServiceEndpointCalled() {
		final String spaceId = "366f90f4-dbe4-44d4-bdde-1c40c69f5274";
		final String instanceId = "123";
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody(serviceDefinitionId, planId, spaceId))
			.put(baseUrl + "/v2/service_instances/{instance_id}", instanceId)
			.then()
			.statusCode(HttpStatus.CREATED.value());

		given()
			.get(baseCfUrl + "/v2/spaces/{space_guid}/apps", spaceId)
			.then()
			.body("resources[0].entity.name", is(equalTo("helloworldapp")))
			.statusCode(200);
	}

	private static String createDefaultBody(String serviceDefinitionId, String planId, String spaceId) {
		final String orgId = "org-guid-here";
		return "{\n" +
			"  \"service_id\": \"" + serviceDefinitionId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"" + orgId + "\",\n" +
			"  \"space_guid\": \"" + spaceId + "\"\n" +
			"}\n";
	}

	private static void setupHoverfly() throws IOException {
		final HoverflyConfig hoverflyConfig = new LocalHoverflyConfig().asWebServer().proxyPort(SIMULATED_CF_PORT).adminPort(8888);
		hoverfly = new Hoverfly(hoverflyConfig, HoverflyMode.SIMULATE);
		hoverfly.start();

		final String hoverflyHost = "localhost";
		final int hoverflyAdminPort = hoverfly
			.getHoverflyConfig()
			.getAdminPort();
		ClassLoader classLoader = CreateInstanceComponentTest.class.getClassLoader();
		File requestsFile = new File(Objects.requireNonNull(classLoader.getResource("requests.json")).getFile());

		given()
			.body(new String(Files.readAllBytes(Paths.get(requestsFile.toURI()))))
			.queryParam("tags", "")
			.put(format("http://%s:%d/api/v2/simulation", hoverflyHost, hoverflyAdminPort))
			.then()
			.statusCode(200);
	}
}