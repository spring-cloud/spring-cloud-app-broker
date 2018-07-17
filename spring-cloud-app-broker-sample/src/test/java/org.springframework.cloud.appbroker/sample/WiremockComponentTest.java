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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import io.restassured.http.Header;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.sample.transformers.URLLocalhostStubResponseTransformer;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.springframework.cloud.appbroker.sample.WiremockComponentTest.SIMULATED_CF_HOST;
import static org.springframework.cloud.appbroker.sample.WiremockComponentTest.SIMULATED_CF_PORT;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {AppBrokerSampleApplication.class},
	properties = {
		"spring.cloud.appbroker.deployer.cloudfoundry.apiHost=" + SIMULATED_CF_HOST,
		"spring.cloud.appbroker.deployer.cloudfoundry.apiPort=" + SIMULATED_CF_PORT,
		"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
		"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
		"spring.cloud.appbroker.deployer.cloudfoundry.defaultOrg=test",
		"spring.cloud.appbroker.deployer.cloudfoundry.defaultSpace=development",
		"spring.cloud.appbroker.deployer.cloudfoundry.secure=false"
	})
@ActiveProfiles("openservicebroker-catalog")
@DirtiesContext
class WiremockComponentTest {

	final static String SIMULATED_CF_HOST = "http://localhost";
	final static int SIMULATED_CF_PORT = 8080;

	static final String SPACE_ID = "ba339810-ca26-4004-b43b-ca859814900f";
	private static final String ACCESS_TOKEN = "an.access.token";

	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	String planId;
	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	@Value("${local.server.port}")
	private String port;
	String baseCfUrl;
	String baseUrl;

	private WireMockServer wiremockServer;

	@BeforeEach
	void setUp(TestInfo testInfo) {
		baseUrl = "http://localhost:" + port;
		baseCfUrl = SIMULATED_CF_HOST + ":" + SIMULATED_CF_PORT;

		startWiremock(getTestMappings(testInfo));
	}

	@AfterEach
	void tearDown() {
		stopWiremock();
	}

	private void startWiremock(String displayName) {
		wiremockServer = new WireMockServer(wireMockConfig()
			.port(SIMULATED_CF_PORT)
			.usingFilesUnderDirectory(displayName)
			.extensions(URLLocalhostStubResponseTransformer.class.getName()));

		wiremockServer.start();

		stubUAA();
	}

	private void stopWiremock() {
		wiremockServer.stop();
	}

	Header getAuthorizationHeader() {
		return new Header("Authorization", "bearer " + ACCESS_TOKEN);
	}

	private static String getTestMappings(TestInfo testInfo) {
		return "src/test/resources/recordings/" + testInfo.getDisplayName().replace("()", "") + "/";
	}

	private static void stubUAA() {
		String refreshToken = "a.refresh.token";

		stubFor(any(urlPathEqualTo("/oauth/token"))
			.willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeaders(createResponseHeaders())
				.withBody("{" +
					"\"access_token\":\"" + ACCESS_TOKEN + "\"" +
					",\"token_type\":\"bearer\"" +
					",\"refresh_token\":\"" + refreshToken + "\"" +
					",\"expires_in\":7199" +
					",\"scope\":\"network.write cloud_controller.admin " +
					"routing.router_groups.read cloud_controller.write " +
					"network.admin doppler.firehose openid routing.router_groups.write " +
					"scim.read uaa.user cloud_controller.read password.write scim.write\"" +
					",\"authorization_endpoint\":\"http://localhost\"" +
					",\"jti\":\"287353917d704131a78967c13623a705\"" +
					"}")
			)
		);
	}

	private static HttpHeaders createResponseHeaders() {
		return HttpHeaders.noHeaders()
			.plus(new HttpHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
		);
	}

	String createDefaultBody() {
		return "{\n" +
			"  \"service_id\": \"" + serviceDefinitionId + "\",\n" +
			"  \"plan_id\": \"" + planId + "\",\n" +
			"  \"organization_guid\": \"org-guid-here\",\n" +
			"  \"space_guid\": \"" + SPACE_ID + "\"\n" +
			"}\n";
	}
}
