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

package org.springframework.cloud.appbroker.component;

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
import org.springframework.cloud.appbroker.TestApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static io.restassured.RestAssured.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.cloud.appbroker.component.ProvisionInstanceComponentTest.ProvisionInstanceComponentTestConfig.SIMULATED_CF_HOST;
import static org.springframework.cloud.appbroker.component.ProvisionInstanceComponentTest.ProvisionInstanceComponentTestConfig.SIMULATED_CF_PORT;

// NB: this can't be in the core subproject. the dependency graph for subprojects is:
//           -deployer
//           /      \
//       -core    -deployer-cloudfoundry
//         /
//   -autoconfigure
// this test requires full autoconfiguration, sample configuration, and a sample SpringBootApplication, so it's
// well beyond the scope of what can be in core. It might be best to have a separate

/**
 * This is a black box test to validate the end-to-end flow of deploying an application from a service broker request.
 * The black box test validates the flow has been performed and external contracts are satisfied but does not
 * rely on external dependencies (eg pushing an app to a real CF)
 * {@see https://github.com/spring-cloud-incubator/spring-cloud-app-broker/issues/4}
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {ProvisionInstanceComponentTest.ProvisionInstanceComponentTestConfig.class, TestApplication.class},
	properties = {
		/*
		 * TODO we should get the jar remotely but see
		 * {@link org.springframework.cloud.appbroker.deployer.cloudfoundry.AbstractCloudFoundryReactiveAppDeployer.getApplication}
		 */
		"spring.cloud.appbroker.deploy.path=classpath:demo.jar",
		"spring.cloud.appbroker.deploy.appName=helloworldapp",
		// TODO not hardcode, we should be stubbing this with fake data
		"spring.cloud.appbroker.cf.apiHost=" + SIMULATED_CF_HOST,
		"spring.cloud.appbroker.cf.apiPort=" + SIMULATED_CF_PORT,
		"spring.cloud.appbroker.cf.username=admin",
		"spring.cloud.appbroker.cf.password=adminpass",
		"spring.cloud.appbroker.cf.defaultOrg=test",
		"spring.cloud.appbroker.cf.defaultSpace=development",
		"spring.cloud.appbroker.cf.secure=false"

	})
public class ProvisionInstanceComponentTest {

	private static final String spaceId = "366f90f4-dbe4-44d4-bdde-1c40c69f5274";
	private static Hoverfly hoverfly;
	private String baseCfUrl;
	private String baseUrl = "";
	private String localhost = "http://localhost";
	@Value("${local.server.port}")
	private String port;

	private static String createDefaultBody() {
		final String serviceId = "bdb1be2e-360b-495c-8115-d7697f9c6a9e";
		final String planId = "b973fb78-82f3-49ef-9b8b-c1876974a6cd";
		final String orgId = "org-guid-here";
		return "{\n" +
			"  \"service_id\": \"" + serviceId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"" + orgId + "\",\n" +
			"  \"space_guid\": \"" + spaceId + "\"\n" +
			"}\n";
	}

	@BeforeAll
	static void setUpClass() throws IOException {
		setupHoverfly();
	}

	private static void setupHoverfly() throws IOException {
		final HoverflyConfig hoverflyConfig = new LocalHoverflyConfig().asWebServer().proxyPort(SIMULATED_CF_PORT).adminPort(8888);
		hoverfly = new Hoverfly(hoverflyConfig, HoverflyMode.SIMULATE);
		hoverfly.start();

		final String hoverflyHost = "localhost";
		final int hoverflyAdminPort = hoverfly
			.getHoverflyConfig()
			.getAdminPort();
		ClassLoader classLoader = ProvisionInstanceComponentTest.class.getClassLoader();
		File requestsFile = new File(Objects.requireNonNull(classLoader.getResource("requests.json")).getFile());

		given()
			.body(new String(Files.readAllBytes(Paths.get(requestsFile.toURI()))))
			.queryParam("tags", "")
			.put(format("http://%s:%d/api/v2/simulation", hoverflyHost, hoverflyAdminPort))
			.then()
			.statusCode(200);
	}

	@AfterAll
	static void tearDownClass() {
		hoverfly.close();
	}

	@BeforeEach
	void setUp() {
		baseUrl = localhost + ":" + port;
		baseCfUrl = localhost + ":" + SIMULATED_CF_PORT;
	}

	@Test
	void shouldPushAppWhenCreateServiceEndpointCalled() {
		final String instanceId = "123";
		given()
			.contentType(ContentType.JSON)
			.body(createDefaultBody())
			.put(baseUrl + "/v2/service_instances/{instance_id}", instanceId)
			.then()
			.statusCode(HttpStatus.CREATED.value());

		given()
			.get(baseCfUrl + "/v2/spaces/{space_guid}/apps", spaceId)
			.then()
			.body("resources[0].entity.name", is(equalTo("helloworldapp")))
			.statusCode(200);
	}

	@Configuration
	static class ProvisionInstanceComponentTestConfig {

		final static String SIMULATED_CF_HOST = "http://localhost";
		final static int SIMULATED_CF_PORT = 8500;
	}
}