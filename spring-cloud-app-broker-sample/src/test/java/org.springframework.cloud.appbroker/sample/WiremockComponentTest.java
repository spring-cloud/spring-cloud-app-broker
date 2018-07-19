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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.sample.fixtures.CloudFoundryApiFixture;
import org.springframework.cloud.appbroker.sample.fixtures.OpenServiceBrokerApiFixture;
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

@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {AppBrokerSampleApplication.class,
		OpenServiceBrokerApiFixture.class,
		CloudFoundryApiFixture.class},
	properties = {
		"spring.cloud.appbroker.deployer.cloudfoundry.api-host=localhost",
		"spring.cloud.appbroker.deployer.cloudfoundry.api-port=8080",
		"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
		"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-org=test",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-space=development",
		"spring.cloud.appbroker.deployer.cloudfoundry.secure=false",
		"wiremock.cloudfoundry.access-token=an.access.token"
	}
)
@ActiveProfiles("openservicebroker-catalog")
@DirtiesContext
class WiremockComponentTest {

	static final String SPACE_ID = "ba339810-ca26-4004-b43b-ca859814900f";

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-host}")
	private String cfApiHost;

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-port}")
	private int cfApiPort;

	@Value("${wiremock.cloudfoundry.access-token}")
	private String accessToken;

	private WireMockServer wiremockServer;

	@BeforeEach
	void setUp(TestInfo testInfo) {
		startWiremock(getTestMappings(testInfo));
	}

	@AfterEach
	void tearDown() {
		stopWiremock();
	}

	private void startWiremock(String displayName) {
		wiremockServer = new WireMockServer(wireMockConfig()
			.port(cfApiPort)
			.usingFilesUnderDirectory(displayName)
			.extensions(URLLocalhostStubResponseTransformer.class.getName()));

		wiremockServer.start();

		stubUAA();
	}

	private void stopWiremock() {
		wiremockServer.stop();
	}

	private static String getTestMappings(TestInfo testInfo) {
		return "src/test/resources/recordings/" + testInfo.getDisplayName().replace("()", "") + "/";
	}

	private void stubUAA() {
		String refreshToken = "a.refresh.token";

		stubFor(any(urlPathEqualTo("/oauth/token"))
			.willReturn(aResponse()
				.withStatus(HttpStatus.OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON.toString())
				.withBody("{" +
					"\"access_token\":\"" + accessToken + "\"" +
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
}
