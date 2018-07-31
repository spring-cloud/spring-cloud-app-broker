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

package org.springframework.cloud.appbroker.acceptance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryClientConfiguration;
import org.springframework.cloud.appbroker.acceptance.fixtures.cf.CloudFoundryService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.util.function.Tuple2;

@SpringBootTest(classes = {CloudFoundryClientConfiguration.class, CloudFoundryService.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(BrokerPropertiesParameterResolver.class)
class CloudFoundryAcceptanceTest {

	@BeforeEach
	void setUp(BrokerProperties brokerProperties) {
		initializeBroker(brokerProperties.getProperties());
	}

	private static final String SAMPLE_BROKER_APP_NAME = "sample-broker";
	private static final String BROKER_NAME = "sample-broker-name";
	private static final String SERVICE_NAME = "example";
	private static final String PLAN_NAME = "standard";
	private static final String SERVICE_INSTANCE_NAME = "my-service";

	@Autowired
	private CloudFoundryService cloudFoundryService;

	@Value("${tests.sampleAppPath}")
	private String sampleAppPath;

	@AfterEach
	void tearDown() {
		cleanup();
	}

	void initializeBroker(List<Tuple2<String, String>> properties) {
		cleanup();

		cloudFoundryService.pushAppNoStart(SAMPLE_BROKER_APP_NAME, getSampleAppPath());
		cloudFoundryService.setBrokerAppEnvironment(properties);
		cloudFoundryService.startApplication(SAMPLE_BROKER_APP_NAME);

		String backingAppURL = cloudFoundryService.getApplicationRoute(SAMPLE_BROKER_APP_NAME);
		cloudFoundryService.createServiceBroker(BROKER_NAME, backingAppURL);
		cloudFoundryService.enableServiceBrokerAccess(SERVICE_NAME);
	}

	private void cleanup() {
		cloudFoundryService.deleteServiceInstance(SERVICE_INSTANCE_NAME);
		cloudFoundryService.deleteServiceBroker(BROKER_NAME);
		cloudFoundryService.deleteBackingApp(SAMPLE_BROKER_APP_NAME);
	}

	void createServiceInstance() {
		cloudFoundryService.createServiceInstance(PLAN_NAME, SERVICE_NAME, SERVICE_INSTANCE_NAME);
	}

	void deleteServiceInstance() {
		cloudFoundryService.deleteServiceInstance(SERVICE_INSTANCE_NAME);
	}

	Optional<ApplicationSummary> getApplicationSummaryByName(String appName) {
		List<ApplicationSummary> applicationsAfterDeletion = cloudFoundryService.getApplications();

		return applicationsAfterDeletion.stream()
			.filter(applicationSummary -> appName.equals(applicationSummary.getName()))
			.findFirst();
	}

	ApplicationEnvironments getApplicationEnvironmentByName(String appName) {
		return cloudFoundryService.getApplicationEnvironmentByAppName(appName);
	}

	private Path getSampleAppPath() {
		return Paths.get(sampleAppPath, "");
	}
}