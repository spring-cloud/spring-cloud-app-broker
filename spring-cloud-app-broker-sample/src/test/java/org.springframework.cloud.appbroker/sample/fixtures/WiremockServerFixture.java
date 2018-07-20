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

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.appbroker.sample.transformers.URLLocalhostStubResponseTransformer;
import org.springframework.http.HttpStatus;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@TestComponent
public class WiremockServerFixture {
	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-port}")
	private int cfApiPort;

	@Value("${wiremock.record:false}")
	private boolean wiremockRecord;

	@Value("${wiremock.cloudfoundry.api-url:}")
	private String cfApiUrl;

	@Value("${wiremock.cloudfoundry.access-token:an.access.token}")
	private String accessToken;

	private WireMockServer wiremockServer;

	public void startWiremock(String recordingDirectoryName) {
		wiremockServer = new WireMockServer(wireMockConfig()
			.port(cfApiPort)
			.usingFilesUnderDirectory(recordingDirectoryName)
			.extensions(URLLocalhostStubResponseTransformer.class.getName()));

		if (wiremockRecord) {
			wiremockServer.startRecording(
				recordSpec()
					.forTarget(cfApiUrl)
			);
		}

		wiremockServer.start();

		stubUAA();
	}

	public void stopWiremock() {
		if (wiremockRecord) {
			wiremockServer.stopRecording();
		}

		wiremockServer.stop();
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
