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

import java.util.Map;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import org.json.JSONObject;

import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class CloudControllerStubFixture extends WiremockStubFixture {

	private static final String TEST_SPACE_GUID = "TEST-SPACE-GUID";
	private static final String TEST_ORG_GUID = "TEST-ORG-GUID";
	private static final String TEST_QUOTA_DEFINITION_GUID = "TEST-QUOTA-DEFINITION-GUID";

	public void stubCommonCloudControllerRequests() {
		stubGetPlatformInfo();
		stubFindTestOrg();
		stubFindTestSpace();
		stubFindDomains();
	}

	private void stubGetPlatformInfo() {
		stubFor(get(urlEqualTo("/"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-root"))));

		stubFor(get(urlEqualTo("/v2/info"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-info"))));
	}

	private void stubFindTestOrg() {
		stubFor(get(urlPathEqualTo("/v2/organizations"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-organizations",
					replace("@org-guid", TEST_ORG_GUID)))));
	}

	private void stubFindTestSpace() {
		stubFor(get(urlPathEqualTo("/v2/spaces"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-spaces",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));

		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-space",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));
	}

	private void stubFindDomains() {
		stubFor(get(urlPathEqualTo("/v2/shared_domains"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-shared-domains",
					replace("@org-guid", TEST_ORG_GUID)))));

		stubFor(get(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/private_domains"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/spaces?page=1"))
			.willReturn(ok()
				.withBody(cc("list-spaces",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));

		stubFor(get(urlEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/space_quota_definitions?page=1"))
			.willReturn(ok()
				.withBody(cc("list-organizations-quota",
					replace("@org-guid", TEST_ORG_GUID)))));

		stubFor(get(urlEqualTo("/v2/quota_definitions/" + TEST_QUOTA_DEFINITION_GUID))
			.willReturn(ok()
				.withBody(cc("get-organizations-quota",
					replace("@org-guid", TEST_ORG_GUID)))));
	}

	public void stubAppDoesNotExist(final String appName) {
		stubAppDoesNotExistInSpace(appName, TEST_SPACE_GUID);
	}

	public void stubAppDoesNotExistInSpace(final String appName, final String space) {
		stubFor(get(urlEqualTo("/v2/spaces/" + space + "/apps?q=name:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(post(urlPathEqualTo("/v2/spaces"))
			.willReturn(ok()));

		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps?q=name:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	public void stubAppExists(final String appName) {
		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps?q=name:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("list-space-apps",
					replace("@guid", appGuid(appName)),
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@stack-guid", stackGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/instances"))
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/summary"))
			.willReturn(ok()
				.withBody(cc("get-app-summary",
					replace("@name", appName),
					replace("@guid", appGuid(appName)),
					replace("@stack-guid", stackGuid(appName)),
					replace("@route-guid", routeGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/stats"))
			.willReturn(ok()
				.withBody(cc("get-app-stats",
					replace("@name", appName)))));

		stubFor(get(urlPathEqualTo("/v2/stacks/" + stackGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-stack",
					replace("@guid", stackGuid(appName))))));
	}

	public void stubPushApp(final String appName, ContentPattern<?>... appMetadataPatterns) {
		stubCreateAppMetadata(appName, appMetadataPatterns);
		stubAppAfterCreation(appName, appName);
	}

	public void stubPushAppWithHost(final String appName, final String host, ContentPattern<?>... appMetadataPatterns) {
		stubCreateAppMetadata(appName, appMetadataPatterns);
		stubAppAfterCreation(appName, host);
	}

	private void stubAppAfterCreation(String appName, String host) {
		stubMapRouteToApp(appName, host);
		stubUploadAppBits(appName);
		stubInitializeAppState(appName);
		stubCheckAppState(appName);
	}

	public void stubUpdateApp(final String appName, ContentPattern<?>... appMetadataPatterns) {
		stubUpdateAppMetadata(appName, appMetadataPatterns);
		stubAppAfterCreation(appName, appName);
	}

	private void stubCreateAppMetadata(String appName, ContentPattern<?>... appMetadataPatterns) {
		MappingBuilder mappingBuilder = post(urlEqualTo("/v2/apps"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + appName + "')]"));
		for (ContentPattern<?> appMetadataPattern : appMetadataPatterns) {
			mappingBuilder.withRequestBody(appMetadataPattern);
		}
		stubFor(mappingBuilder
			.willReturn(ok()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubUpdateAppMetadata(String appName, ContentPattern<?>... appMetadataPatterns) {
		MappingBuilder mappingBuilder = put(urlEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + appName + "')]"));
		for (ContentPattern<?> appMetadataPattern : appMetadataPatterns) {
			mappingBuilder.withRequestBody(appMetadataPattern);
		}
		stubFor(mappingBuilder
			.willReturn(ok()
				.withBody(cc("get-app-STARTED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubMapRouteToApp(String appName, String host) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/routes"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlEqualTo("/v2/routes?q=domain_guid:" + TEST_ORG_GUID + "&q=host:" + host + "&page=1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(post(urlEqualTo("/v2/routes"))
			.withRequestBody(matchingJsonPath("$.[?(@.host == '" + host + "')]"))
			.willReturn(created()
				.withBody(cc("list-routes",
					replace("@name", appName),
					replace("@guid", routeGuid(appName))))));

		stubFor(put(urlEqualTo("/v2/apps/" + appGuid(appName) + "/routes/" + routeGuid(appName)))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubUploadAppBits(String appName) {
		stubFor(put(urlEqualTo("/v2/resource_match"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody("[]")));

		stubFor(put(urlEqualTo("/v2/apps/" + appGuid(appName) + "/bits?async=true"))
			.willReturn(created()
				.withBody(cc("put-app-bits",
					replace("@guid", appName + "-JOB-GUID")))));
	}

	private void stubInitializeAppState(String appName) {
		stubFor(put(urlEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STOPPED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));

		stubFor(put(urlEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STARTED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STARTED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubCheckAppState(String appName) {
		stubFor(get(urlEqualTo("/v2/apps/" + appGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-app-STAGED"))));

		stubFor(get(urlEqualTo("/v2/apps/" + appGuid(appName) + "/instances"))
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));
	}

	public void stubDeleteApp(String appName) {
		stubFor(delete(urlPathEqualTo("/v2/routes/" + appName + "-ROUTE-GUID"))
			.willReturn(ok()
				.withBody(cc("get-route",
					replace("@guid", routeGuid(appName))))));

		stubFor(delete(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.willReturn(noContent()));
	}

	public void stubServiceInstanceExists(String serviceInstanceName) {
		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/service_instances" +
			"?q=name:" + serviceInstanceName +
			"&page=1" +
			"&return_user_provided_service_instances=true"))
			.willReturn(ok()
				.withBody(cc("list-space-service_instances",
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@name", serviceInstanceName),
					replace("@guid", serviceInstanceName + "-GUID")))));
	}

	public void stubServiceInstanceDoesNotExists(String serviceInstanceName) {
		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/service_instances" +
			"?q=name:" + serviceInstanceName +
			"&page=1" +
			"&return_user_provided_service_instances=true"))
			.willReturn(ok()
				.withBody(cc("list-space-service_instances-empty"))));
	}

	public void stubServiceExists(String serviceName) {
		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/services" +
			"?q=label:" + serviceName +
			"&page=1"))
			.willReturn(ok()
				.withBody(cc("list-space-services"))));

		stubFor(get(urlEqualTo("/v2/service_plans?q=service_guid:SERVICE-ID&page=1"))
			.willReturn(ok()
				.withBody(cc("list-service-plans"))));
	}

	public void stubCreateServiceInstance(String serviceInstanceName) {
		stubFor(post(urlEqualTo("/v2/service_instances?accepts_incomplete=true"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + serviceInstanceName + "')]"))
			.willReturn(ok()));
	}

	public void stubCreateServiceInstanceWithParameters(String serviceInstanceName, Map<String, Object> params) {
		stubFor(post(urlEqualTo("/v2/service_instances?accepts_incomplete=true"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + serviceInstanceName + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.parameters == " + new JSONObject(params) + ")]"))
			.willReturn(ok()));
	}

	public void stubUpdateServiceInstanceWithParameters(String serviceInstanceName, Map<String, Object> params) {
		String serviceInstanceGuid = serviceInstanceName + "-GUID";
		stubFor(put(urlEqualTo("/v2/service_instances/" + serviceInstanceGuid + "?accepts_incomplete=true"))
			.withRequestBody(matchingJsonPath("$.[?(@.parameters == " + new JSONObject(params) + ")]"))
			.willReturn(ok()));
	}

	public void stubCreateServiceBinding(String appName, String serviceInstanceName) {
		String serviceInstanceGuid = serviceInstanceName + "-GUID";
		String serviceBindingGuid = appGuid(appName) + "-" + serviceInstanceGuid;

		stubFor(post(urlEqualTo("/v2/service_bindings"))
			.withRequestBody(matchingJsonPath("$.[?(@.app_guid == '" + appGuid(appName) + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.service_instance_guid == '" + serviceInstanceGuid + "')]"))
			.willReturn(created()
				.withBody(cc("list-service-bindings",
					replace("@guid", serviceBindingGuid),
					replace("@app-guid", appGuid(appName)),
					replace("@service-instance-guid", serviceInstanceGuid)))));
	}

	public void stubServiceBindingDoesNotExist(String appName) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/service_bindings"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	private String cc(String fileRoot, StringReplacementPair... replacements) {
		String response = readResponseFromFile(fileRoot, "cloudcontroller");
		for (StringReplacementPair pair : replacements) {
			response = response.replaceAll(pair.getRegex(), pair.getReplacement());
		}
		return response;
	}

	private String appGuid(String appName) {
		return appName + "-GUID";
	}

	private String routeGuid(String appName) {
		return appName + "-ROUTE-GUID";
	}

	private String stackGuid(String appName) {
		return appName + "-STACK-GUID";
	}
}
