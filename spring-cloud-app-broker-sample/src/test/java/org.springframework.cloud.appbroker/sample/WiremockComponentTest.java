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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.sample.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.sample.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.appbroker.sample.fixtures.UaaStubFixture;
import org.springframework.cloud.appbroker.sample.fixtures.WiremockServerFixture;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {AppBrokerSampleApplication.class,
		WiremockServerFixture.class,
		OpenServiceBrokerApiFixture.class,
		CloudControllerStubFixture.class,
		UaaStubFixture.class},
	properties = {
		"spring.cloud.appbroker.deployer.cloudfoundry.api-host=localhost",
		"spring.cloud.appbroker.deployer.cloudfoundry.api-port=8080",
		"spring.cloud.appbroker.deployer.cloudfoundry.username=admin",
		"spring.cloud.appbroker.deployer.cloudfoundry.password=adminpass",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-org=test",
		"spring.cloud.appbroker.deployer.cloudfoundry.default-space=development",
		"spring.cloud.appbroker.deployer.cloudfoundry.secure=false"
	}
)
@ActiveProfiles("openservicebroker-catalog")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WiremockComponentTest {

	@Autowired
	private WiremockServerFixture wiremockFixture;

	@Autowired
	private CloudControllerStubFixture cloudFoundryFixture;

	@Autowired
	private UaaStubFixture uaaFixture;

	@BeforeAll
	void setUp() {
		wiremockFixture.startWiremock();
	}

	@AfterAll
	void tearDown() {
		wiremockFixture.stopWiremock();
	}

	@BeforeEach
	void resetWiremock() {
		wiremockFixture.resetWiremock();

		uaaFixture.stubCommonUaaRequests();
		cloudFoundryFixture.stubCommonCloudControllerRequests();
	}

	@AfterEach
	void verifyStubs() {
		wiremockFixture.verifyAllRequiredStubsUsed();
	}
}
