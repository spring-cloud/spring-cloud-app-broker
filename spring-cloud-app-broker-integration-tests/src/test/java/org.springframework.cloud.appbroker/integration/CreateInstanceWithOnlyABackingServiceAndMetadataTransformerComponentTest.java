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

package org.springframework.cloud.appbroker.integration;

import java.util.HashMap;
import java.util.Map;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Hooks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.CreateBackingServicesMetadataTransformationServiceImpl;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SI_NAME;

@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard"
})
@ContextConfiguration(classes = CreateInstanceWithOnlyABackingServiceAndMetadataTransformerComponentTest.CustomConfig.class)
class CreateInstanceWithOnlyABackingServiceAndMetadataTransformerComponentTest extends WiremockComponentTest {

	protected static final String APP_NAME = "app-with-new-services";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@BeforeAll
	void setUpReactorDebugging() {
		Hooks.onOperatorDebug();
	}



	@Test
	void createsServices_When_OnlyBackingServiceIsRequested_WithoutContext_SetsMetadata() {
		Map<String, Object> expectedLabels = new HashMap<>();
		expectedLabels.put("brokered_service_instance_guid","instance-id");
		expectedLabels.put("backing_service_instance_guid", "my-db-service-GUID");
		Map<String, Object> expectedAnnotations = new HashMap<>();

		assertsCreateAndMetadataAssigned(brokerFixture.serviceInstanceRequest(), expectedLabels, expectedAnnotations);
	}

	@Test
	void createsServices_When_OnlyBackingServiceIsRequested_WithK8SContext_SetsMetadata() {
		String context = "\"context\": {\n" +
			"  \"platform\": \"kubernetes\",\n" +
			"  \"namespace\": \"development\",\n" +
			"  \"clusterid\": \"8263feba-9b8a-23ae-99ed-abcd1234feda\"\n" +
			"}";

		Map<String, Object> expectedLabels = new HashMap<>();
		expectedLabels.put("brokered_service_instance_guid","instance-id");
		expectedLabels.put("backing_service_instance_guid", "my-db-service-GUID");
		expectedLabels.put("brokered_service_context_clusterid", "8263feba-9b8a-23ae-99ed-abcd1234feda");
		//coming from originating identity base64 encoded string
		expectedLabels.put("brokered_service_originating_identity_uid", "c2dde242-5ce4-11e7-988c-000c2946f14f"); //coming from originating identity base64 encoded string
		expectedLabels.put("brokered_service_context_namespace", "development");
		Map<String, Object> expectedAnnotations = new HashMap<>();
		expectedAnnotations.put("brokered_service_originating_identity_groups", "[\"admin\",\"dev\"]"); //coming from originating identity base64 encoded string
		expectedAnnotations.put("brokered_service_originating_identity_extra", "{\"mydata\":[\"data1\",\"data3\"]}"); //coming from originating identity base64 encoded string
		expectedAnnotations.put("brokered_service_originating_identity_username", "duke"); //coming from originating identity base64 encoded string

		assertsCreateAndMetadataAssigned(brokerFixture.serviceInstanceRequestWithK8sOsbContext(context), expectedLabels, expectedAnnotations);
	}

	@Test
	void createsServices_When_OnlyBackingServiceIsRequested_WithCfContext_SetsMetadata() {
		String context = "\"context\": {\n" +
			"  \"platform\": \"cloudfoundry\",\n" +
			"  \"organization_guid\": \"1113aa0-124e-4af2-1526-6bfacf61b111\",\n" +
			"  \"organization_name\": \"system\",\n" +
			"  \"space_guid\": \"aaaa1234-da91-4f12-8ffa-b51d0336aaaa\",\n" +
			"  \"space_name\": \"development\",\n" +
			"  \"instance_name\": \"my-db\"\n" +
			"}";

		Map<String, Object> expectedLabels = new HashMap<>();
		expectedLabels.put("brokered_service_instance_guid","instance-id");
		expectedLabels.put("backing_service_instance_guid", "my-db-service-GUID");
		expectedLabels.put("brokered_service_context_organization_guid", "1113aa0-124e-4af2-1526-6bfacf61b111");
		expectedLabels.put("brokered_service_originating_identity_user_id", "683ea748-3092-4ff4-b656-39cacc4d5360");  //coming from originating identity base64 encoded string
		expectedLabels.put("brokered_service_context_space_guid", "aaaa1234-da91-4f12-8ffa-b51d0336aaaa");

		Map<String, Object> expectedAnnotations = new HashMap<>();
		expectedAnnotations.put("brokered_service_context_instance_name", "my-db");
		expectedAnnotations.put("brokered_service_context_space_name", "development");
		expectedAnnotations.put("brokered_service_context_organization_name", "system");

		assertsCreateAndMetadataAssigned(brokerFixture.serviceInstanceRequestWithCfOsbContext(context), expectedLabels, expectedAnnotations);
	}

	private void assertsCreateAndMetadataAssigned(RequestSpecification serviceInstanceRequest,
		Map<String, Object> labels, Map<String, Object> annotations) {
		// given services are available in the marketplace
		cloudControllerFixture.stubServiceExists(BACKING_SERVICE_NAME);

		// will create the service instance
		cloudControllerFixture.stubCreateServiceInstance(BACKING_SI_NAME);

		// will list the created service instance
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		// will list the created service instance bindings with no results
		cloudControllerFixture.stubListServiceBindingsWithNoResult(BACKING_SI_NAME);

		// will update the metadata on the service instance
		// results
		cloudControllerFixture.stubUpdateServiceInstanceMetadata(BACKING_SI_NAME, labels, annotations);

		// when a service instance is created
		given(serviceInstanceRequest)
			.when()
			.put(brokerFixture.createServiceInstanceUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.ACCEPTED.value());

		// when the "last_operation" API is polled
		given(brokerFixture.serviceInstanceRequest())
			.when()
			.get(brokerFixture.getLastInstanceOperationUrl(), "instance-id")
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("state", is(equalTo(OperationState.IN_PROGRESS.toString())));

		String state = brokerFixture.waitForAsyncOperationComplete("instance-id");
		assertThat(state).isEqualTo(OperationState.SUCCEEDED.toString());
	}

	@Configuration
	static class CustomConfig {

		@Bean
		@ConditionalOnMissingBean
		public CreateBackingServicesMetadataTransformationService createBackingServicesMetadataTransformationService() {
			return new CreateBackingServicesMetadataTransformationServiceImpl();
		}

	}

}
