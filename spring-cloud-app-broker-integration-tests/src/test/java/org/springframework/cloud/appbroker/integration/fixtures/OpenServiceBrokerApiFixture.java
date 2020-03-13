/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.with;

@TestComponent
public class OpenServiceBrokerApiFixture implements ApplicationListener<ApplicationStartedEvent> {

	private static final String ORG_ID = "org-id";

	private static final String SPACE_ID = "space-id";

	private static final String APP_ID = "app-id";

	private static final String SERVICE_KEY_CLIENT_ID = "service-key-client-id";

	@Value("${spring.cloud.openservicebroker.catalog.services[0].plans[0].id}")
	private String planId;

	@Value("${spring.cloud.openservicebroker.catalog.services[0].id}")
	private String serviceDefinitionId;

	private String port;

	public String createServiceInstanceUrl() {
		return "/service_instances/{instance_id}";
	}

	public String getLastInstanceOperationUrl() {
		return "/service_instances/{instance_id}/last_operation";
	}

	public String deleteServiceInstanceUrl() {
		return "/service_instances/{instance_id}" +
			"?service_id=" + serviceDefinitionId +
			"&plan_id=" + planId;
	}

	public String createBindingUrl() {
		return "/service_instances/{instance_id}/service_bindings/{binding_id}";
	}

	public String deleteBindingUrl() {
		return "/service_instances/{instance_id}/service_bindings/{binding_id}" +
			"?service_id=" + serviceDefinitionId +
			"&plan_id=" + planId;
	}

	public RequestSpecification serviceInstanceRequest() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"" +
				"}\n");
	}

	public RequestSpecification serviceInstanceRequest(Map<String, Object> params) {
		String stringParams = new JSONObject(params).toString();
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"organization_guid\": \"" + ORG_ID + "\"," +
				"\"space_guid\": \"" + SPACE_ID + "\"," +
				"\"parameters\": " + stringParams +
				"}");
	}

	public RequestSpecification serviceAppBindingRequest() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"bind_resource\": {" +
				"\"app_guid\": \"" + APP_ID + "\"" +
				"}" +
				"}");
	}

	public RequestSpecification serviceAppBindingRequest(Map<String, Object> params) {
		String stringParams = new JSONObject(params).toString();
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"bind_resource\": {" +
					"\"app_guid\": \"" + APP_ID + "\"" +
				"}," +
				"\"parameters\": " + stringParams +
				"}");
	}

	//The default binding resource format used by CF, albeit yet undocumented,
	// see https://github.com/openservicebrokerapi/servicebroker/pull/704
	public RequestSpecification serviceKeyBindingRequest() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\"," +
				"\"bind_resource\": {" +
				"\"credential_client_id\": \"" + SERVICE_KEY_CLIENT_ID + "\"" +
				"}" +
				"}");
	}

	//A OSB-API compliant request (bind resource is optional per https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#bind-resource-object
	// "bind_resource and its fields are OPTIONAL"
	public RequestSpecification serviceBindingRequestWithoutResource() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\""+
				"}");
	}

	//A OSB-API compliant request (bind resource is optional per https://github.com/openservicebrokerapi/servicebroker/blob/master/spec.md#bind-resource-object
	// "bind_resource and its fields are OPTIONAL"
	public RequestSpecification serviceBindingRequestWithEmptyResource() {
		return serviceBrokerSpecification()
			.body("{" +
				"\"service_id\": \"" + serviceDefinitionId + "\"," +
				"\"plan_id\": \"" + planId + "\","+
				"\"bind_resource\": {}" +
				"}");
	}

	public RequestSpecification serviceBrokerSpecification() {
		return with()
			.baseUri("http://localhost:" + port + "/v2")
			.accept(ContentType.JSON)
			.contentType(ContentType.JSON);
	}

	public String waitForAsyncOperationComplete(String serviceInstanceId) {
		try {
			String state;
			do {
				Thread.sleep(TimeUnit.SECONDS.toMillis(5));
				state = given(serviceInstanceRequest())
					.when()
					.get(getLastInstanceOperationUrl(), serviceInstanceId)
					.then()
					.statusCode(HttpStatus.OK.value())
					.extract().body().jsonPath().getString("state");
			} while (state.equals(OperationState.IN_PROGRESS.toString()));
			return state;
		}
		catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		this.port = event.getApplicationContext().getEnvironment().getProperty("local.server.port");
	}

}
