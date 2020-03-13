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

import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.appbroker.integration.fixtures.CloudControllerStubFixture;
import org.springframework.cloud.appbroker.integration.fixtures.OpenServiceBrokerApiFixture;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static java.util.Collections.singletonMap;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SERVICE_NAME;
import static org.springframework.cloud.appbroker.integration.CreateInstanceWithServicesComponentTest.BACKING_SI_NAME;


@TestPropertySource(properties = {
	"spring.cloud.appbroker.services[0].service-name=example",
	"spring.cloud.appbroker.services[0].plan-name=standard",
	"spring.cloud.appbroker.services[0].services[0].service-instance-name=" + BACKING_SI_NAME,
	"spring.cloud.appbroker.services[0].services[0].name=" + BACKING_SERVICE_NAME,
	"spring.cloud.appbroker.services[0].services[0].plan=standard",
	"service-bindings-as-service-keys=true"
})
class CreateBindingWithServiceKeyComponentTest extends WiremockComponentTest {

	private static final String SERVICE_INSTANCE_ID = "instance-id";
	private static final String BINDING_ID = "binding-id";

	protected static final String APP_NAME = "app-with-new-services";

	protected static final String BACKING_SI_NAME = "my-db-service";

	protected static final String BACKING_SERVICE_NAME = "db-service";

	@Autowired
	private CloudControllerStubFixture cloudControllerFixture;

	@Autowired
	private OpenServiceBrokerApiFixture brokerFixture;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	String serviceDefinitionId;

	@Test
	void createServiceKeyReturnsCredentialsFromServiceKey() {

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		//given service key creation request gets accepted, and returns credentials
		cloudControllerFixture.stubCreateServiceKey(BACKING_SI_NAME, BINDING_ID, new HashMap<>());

		//given service key gets properly read
		cloudControllerFixture.stubListServiceKey(BACKING_SI_NAME, BINDING_ID);

		// when a service key is created (using CF profile)
		given(brokerFixture.serviceKeyBindingRequest())
			.filter(new RequestLoggingFilter())
			.filter(new ResponseLoggingFilter())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body( //service key credentials are properly returned
				"credentials",
				equalTo(singletonMap("creds-key-43", "creds-val-43"))
			);


		// when a service key is created by a custom OSB compliant client with empty  binding resource
		given(brokerFixture.serviceBindingRequestWithEmptyResource())
			.filter(new RequestLoggingFilter())
			.filter(new ResponseLoggingFilter())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body( //service key credentials are properly returned
				"credentials",
				equalTo(singletonMap("creds-key-43", "creds-val-43"))
			);

		// when a service key is created a custom OSB compliant client without binding resource
		given(brokerFixture.serviceBindingRequestWithoutResource())
			.filter(new ResponseLoggingFilter())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body( //service key credentials are properly returned
				"credentials",
				equalTo(singletonMap("creds-key-43", "creds-val-43"))
			);

	}

	@Test
	void createServiceBindindReturnsCredentialsFromServiceKey() {

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		//given service key creation request gets accepted, and returns credentials
		cloudControllerFixture.stubCreateServiceKey(BACKING_SI_NAME, BINDING_ID, new HashMap<>());

		//given service key gets properly read
		cloudControllerFixture.stubListServiceKey(BACKING_SI_NAME, BINDING_ID);

		// when a service binding is created
		given(brokerFixture.serviceAppBindingRequest())
			.filter(new RequestLoggingFilter())
			.filter(new ResponseLoggingFilter())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body( //service key credentials are properly returned
				"credentials",
				equalTo(singletonMap("creds-key-43", "creds-val-43"))
			);

	}


	@Test
	void createServiceBindindWithParamsCreatesServiceKeyWithParams() {

		// given services are available in the marketplace
		cloudControllerFixture.stubServiceInstanceExists(BACKING_SI_NAME);

		//given service key creation request gets accepted, and returns credentials
		cloudControllerFixture.stubCreateServiceKey(BACKING_SI_NAME, BINDING_ID, singletonMap("a-key", "a-value"));

		//given service key gets properly read
		cloudControllerFixture.stubListServiceKey(BACKING_SI_NAME, BINDING_ID);

		// when a service binding is created
		given(brokerFixture.serviceAppBindingRequest(singletonMap("a-key", "a-value")))
			.filter(new RequestLoggingFilter())
			.filter(new ResponseLoggingFilter())
			.when()
			.put(brokerFixture.createBindingUrl(), SERVICE_INSTANCE_ID, BINDING_ID)
			.then()
			.statusCode(HttpStatus.CREATED.value())
			.body( //service key credentials are properly returned
				"credentials",
				equalTo(singletonMap("creds-key-43", "creds-val-43"))
			);

	}



}