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
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.recordSpec;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

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

	public void startWiremock() {
		wiremockServer = new WireMockServer(wireMockConfig()
			.port(cfApiPort)
			.usingFilesUnderClasspath("recordings"));

		if (wiremockRecord) {
			wiremockServer.startRecording(
				recordSpec()
					.forTarget(cfApiUrl)
			);
		}

		wiremockServer.start();
	}

	public void stopWiremock() {
		if (wiremockRecord) {
			wiremockServer.stopRecording();
		}

		wiremockServer.stop();
	}

	public void verifyAllRequiredStubsUsed() {
		Set<UUID> servedStubIds = wiremockServer.getServeEvents().getRequests().stream()
			.filter(event -> event.getStubMapping() != null)
			.map(event -> event.getStubMapping().getId())
			.collect(Collectors.toSet());

		List<StubMapping> unusedStubs = wiremockServer.listAllStubMappings().getMappings().stream()
			.filter(stub -> !servedStubIds.contains(stub.getId()))
			.filter(this::stubIsOptional)
			.collect(Collectors.toList());

		assertThat(unusedStubs.size())
			.as("%d wiremock stubs were unused. Unused stubs are: %s", unusedStubs.size(), unusedStubs)
			.isEqualTo(0);
	}

	private boolean stubIsOptional(StubMapping stub) {
		if (stub.getMetadata() != null) {
			return !stub.getMetadata().getBoolean("optional");
		}
		return false;
	}
}
