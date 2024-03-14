/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.cloud.appbroker.integration;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.appbroker.deployer.DeploymentProperties;
import org.springframework.cloud.appbroker.extensions.targets.ArtifactDetails;
import org.springframework.cloud.appbroker.extensions.targets.Target;
import org.springframework.cloud.appbroker.extensions.targets.TargetFactory;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithCustomTargetComponentTest.APP_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithCustomTargetComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithCustomTargetComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].apps[0].path=classpath:demo.jar",
	"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
	"spring.cloud.appbroker.services[0].apps[0].services[0].service-instance-name=" + BACKING_SI_NAME,

	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",

	"spring.cloud.appbroker.services[0].target.name=CustomSpaceTarget"
})
@ContextConfiguration(classes = CreateInstanceWithCustomTargetComponentTest.CustomConfig.class)
class CreateInstanceWithCustomTargetComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-new-services-target";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	protected static final String BACKING_PLAN_NAME = "standard";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Test
	void pushAppWithServicesInSpace() {
		String serviceInstanceId = "instance-id";
		String customSpace = "my-space";
		String customSpaceGuid = "my-space-guid";

		cloudControllerFixture.stubFindTestOrg();
		cloudControllerFixture.stubFindSpaceV3(customSpace, customSpaceGuid);
		cloudControllerFixture.stubCreateSpace(customSpace, customSpaceGuid);
		cloudControllerFixture.stubAssociatePermissions();
		cloudControllerFixture.stubPushApp(APP_NAME);

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExistsInSpace(BACKING_SERVICE_NAME, BACKING_PLAN_NAME, customSpaceGuid);

		// will create and bind the service instance
		cloudControllerFixture.stubCreateServiceInstance(BACKING_SI_NAME);
		cloudControllerFixture.stubCreateServiceBinding(APP_NAME, BACKING_SI_NAME);
		cloudControllerFixture.stubServiceInstanceExistsInSpace(BACKING_SI_NAME, customSpaceGuid);

		// when a service instance is created
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), serviceInstanceId)
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), serviceInstanceId)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete(serviceInstanceId);
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

	@Configuration
	protected static class CustomConfig {

		@Bean
		public CustomSpaceService customSpaceService() {
			return new CustomSpaceService();
		}

		@Bean
		public CustomSpaceTarget customSpaceTarget(CustomSpaceService customSpaceService) {
			return new CustomSpaceTarget(customSpaceService);
		}

		@SuppressWarnings({"PMD.UnusedFormalParameter"})
		static final class CustomSpaceTarget extends TargetFactory<CustomSpaceTarget.Config> {

			private final CustomSpaceService customSpaceService;

			public CustomSpaceTarget(CustomSpaceService customSpaceService) {
				super(Config.class);
				this.customSpaceService = customSpaceService;
			}

			@Override
			public Target create(Config config) {
				return this::apply;
			}

			private ArtifactDetails apply(Map<String, String> properties, String name, String serviceInstanceId) {
				String space = customSpaceService.retrieveSpaceName();
				properties.put(DeploymentProperties.TARGET_PROPERTY_KEY, space);

				return ArtifactDetails.builder()
					.name(name)
					.properties(properties)
					.build();
			}

			public static class Config {
			}

		}

		static class CustomSpaceService {

			String retrieveSpaceName() {
				return "my-space";
			}

		}

	}

}
