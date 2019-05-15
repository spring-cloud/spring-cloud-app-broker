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

package org.springframework.cloud.appbroker.acceptance;

import java.util.Optional;

import com.jayway.jsonpath.DocumentContext;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.uaa.clients.GetClientResponse;
import org.cloudfoundry.uaa.tokens.GrantType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInstanceWithOAuth2CredentialsAcceptanceTest extends CloudFoundryAcceptanceTest {

	private static final String APP_NAME = "app-create-oauth2";
	private static final String SI_NAME = "si-create-oauth2";

	private static final String SUFFIX = "create-instance-oauth2";
	private static final String APP_SERVICE_NAME = "app-service-"+ SUFFIX;
	private static final String BACKING_SERVICE_NAME = "backing-service-"+ SUFFIX;

	@Override
	protected String testSuffix() {
		return SUFFIX;
	}

	@Override
	protected String appServiceName() {
		return APP_SERVICE_NAME;
	}

	@Override
	protected String backingServiceName() {
		return BACKING_SERVICE_NAME;
	}

	@Test
	@AppBrokerTestProperties({
		"spring.cloud.appbroker.services[0].service-name=" + APP_SERVICE_NAME,
		"spring.cloud.appbroker.services[0].plan-name=" + PLAN_NAME,

		"spring.cloud.appbroker.services[0].apps[0].name=" + APP_NAME,
		"spring.cloud.appbroker.services[0].apps[0].path=" + BACKING_APP_PATH,
		
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].name=SpringSecurityOAuth2",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.registration=sample-app-client",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.grant-types=[\"client_credentials\"]",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.authorities=[\"uaa.resource\"]",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.length=12",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-uppercase-alpha=false",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-lowercase-alpha=true",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-numeric=false",
		"spring.cloud.appbroker.services[0].apps[0].credential-providers[0].args.include-special=false"
	})
	void deployAppsWithOAuth2OnCreateService() {
		// when a service instance is created
		createServiceInstance(SI_NAME);

		String serviceInstanceGuid = getServiceInstanceGuid(SI_NAME);

		// then a backing application is deployed
		Optional<ApplicationSummary> backingApplication = getApplicationSummary(APP_NAME);
		assertThat(backingApplication).hasValueSatisfying(app ->
			assertThat(app.getRunningInstances()).isEqualTo(1));

		// and has the environment variables
		DocumentContext json = getSpringAppJson(APP_NAME);
		assertThat(json.read("$.['spring.security.oauth2.client.registration.sample-app-client.client-id']").toString())
			.isEqualTo(uaaClientId(serviceInstanceGuid));
		assertThat(json.read("$.['spring.security.oauth2.client.registration.sample-app-client.client-secret']").toString())
			.matches("[a-zA-Z]{12}");

		// and a UAA client is created
		Optional<GetClientResponse> uaaClient = getUaaClient(uaaClientId(serviceInstanceGuid));
		assertThat(uaaClient).hasValueSatisfying(client -> {
			assertThat(client.getAuthorities()).contains("uaa.resource");
			assertThat(client.getAuthorizedGrantTypes()).contains(GrantType.CLIENT_CREDENTIALS);
		});

		// when the service instance is deleted
		deleteServiceInstance(SI_NAME);

		// then the backing application is deleted
		Optional<ApplicationSummary> backingApplicationAfterDeletion = getApplicationSummary(APP_NAME);
		assertThat(backingApplicationAfterDeletion).isEmpty();

		// and the UAA client is deleted
		Optional<GetClientResponse> uaaClientAfterDeletion = getUaaClient(uaaClientId(serviceInstanceGuid));
		assertThat(uaaClientAfterDeletion).isEmpty();
	}

	private String uaaClientId(String serviceInstanceGuid) {
		return APP_NAME + "-" + serviceInstanceGuid;
	}
}