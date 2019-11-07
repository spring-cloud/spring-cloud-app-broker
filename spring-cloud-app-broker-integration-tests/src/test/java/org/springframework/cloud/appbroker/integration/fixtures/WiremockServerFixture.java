/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.appbroker.integration.fixtures;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@TestComponent
public class WiremockServerFixture {

	@Value("${spring.cloud.appbroker.deployer.cloudfoundry.api-port}")
	private int cfApiPort;

	private WireMockServer ccUaaWiremockServer;

	private WireMockServer credHubWiremockServer;

	public void startWiremock() {
		ccUaaWiremockServer = new WireMockServer(wireMockConfig()
			.port(cfApiPort)
			.usingFilesUnderClasspath("recordings"));
		ccUaaWiremockServer.start();

		credHubWiremockServer = new WireMockServer(wireMockConfig()
			.port(8888)
			.usingFilesUnderClasspath("recordings"));
		credHubWiremockServer.start();
	}

	public void stopWiremock() {
		ccUaaWiremockServer.stop();
		credHubWiremockServer.stop();
	}

	public void resetWiremock() {
		ccUaaWiremockServer.resetAll();
		credHubWiremockServer.resetAll();
	}

	public void verifyAllRequiredStubsUsed() {
		verifyStubs(ccUaaWiremockServer);
		verifyStubs(credHubWiremockServer);
	}

	private void verifyStubs(WireMockServer wireMockServer) {
		Set<UUID> servedStubIds = wireMockServer.getServeEvents().getRequests().stream()
			.filter(event -> event.getStubMapping() != null)
			.map(event -> event.getStubMapping().getId())
			.collect(Collectors.toSet());

		List<StubMapping> unusedStubs = wireMockServer.listAllStubMappings().getMappings().stream()
			.filter(stub -> !servedStubIds.contains(stub.getId()))
			.filter(this::stubIsRequired)
			.collect(Collectors.toList());

		assertThat(unusedStubs.size())
			.as("%d wiremock stubs were unused. Unused stubs are: %s", unusedStubs.size(), unusedStubs)
			.isEqualTo(0);
	}

	private boolean stubIsRequired(StubMapping stub) {
		if (stub.getMetadata() != null) {
			return !stub.getMetadata().getBoolean("optional");
		}
		return true;
	}

}
