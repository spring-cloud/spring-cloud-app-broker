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

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import org.json.JSONObject;

import org.springframework.boot.test.context.TestComponent;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

@TestComponent
public class CloudControllerStubFixture extends WiremockStubFixture {

	private static final String TEST_SPACE_GUID = "TEST-SPACE-GUID";

	private static final String TEST_ORG_GUID = "TEST-ORG-GUID";

	private static final String TEST_QUOTA_DEFINITION_GUID = "TEST-QUOTA-DEFINITION-GUID";

	protected CloudControllerStubFixture() {
		super(8080);
	}

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

		stubFor(get(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-organization",
					replace("@org-guid", TEST_ORG_GUID)))));
	}

	private void stubFindTestSpace() {
		stubFor(get(urlPathEqualTo("/v2/spaces"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-spaces",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));

		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-space",
					replace("@name", "test"),
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));

		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps"))
			.withQueryParam("page", equalTo("1"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/security_groups"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-space-security_groups"))));
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

		stubFor(get(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/spaces"))
			.withQueryParam("page", equalTo("1"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-spaces",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID)))));

		stubFor(get(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/space_quota_definitions"))
			.withQueryParam("page", equalTo("1"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-organizations-quota",
					replace("@org-guid", TEST_ORG_GUID)))));

		stubFor(get(urlPathEqualTo("/v2/quota_definitions/" + TEST_QUOTA_DEFINITION_GUID))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-organizations-quota",
					replace("@org-guid", TEST_ORG_GUID)))));
	}

	public void stubCreateSpace(final String spaceName) {
		stubFor(post(urlPathEqualTo("/v2/spaces"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + spaceName + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.organization_guid == '" + TEST_ORG_GUID + "')]"))
			.willReturn(ok()
				.withBody(cc("get-space",
					replace("@name", spaceName),
					replace("@space-guid", "CREATED-SPACE-GUID"),
					replace("@org-guid", TEST_ORG_GUID)))));
	}

	public void stubAppDoesNotExist(final String appName) {
		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps"))
			.withQueryParam("q", equalTo("name:" + appName))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	public void stubAppExists(final String appName) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-app-STAGED",
					replace("@name", appName)))));

		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps"))
			.withQueryParam("q", equalTo("name:" + appName))
			.withQueryParam("page", equalTo("1"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("list-space-apps",
					replace("@name", appName),
					replace("@guid", appGuid(appName)),
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@stack-guid", stackGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/instances"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/summary"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-app-summary",
					replace("@name", appName),
					replace("@guid", appGuid(appName)),
					replace("@stack-guid", stackGuid(appName)),
					replace("@route-guid", routeGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/stats"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-app-stats",
					replace("@name", appName)))));

		stubFor(get(urlPathEqualTo("/v2/stacks/" + stackGuid(appName)))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-stack",
					replace("@guid", stackGuid(appName))))));
	}

	public void stubAppExistsWithBackingService(final String appName, final String serviceInstanceName,
		final String serviceName, final String planName) {
		stubAppExists(appName);

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/summary"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("get-app-summary-with-backing-service",
					replace("@name", appName),
					replace("@service-instance-name", serviceInstanceName),
					replace("@service-guid", serviceGuid(serviceName)),
					replace("@plan-guid", planGuid(planName)),
					replace("@guid", appGuid(appName)),
					replace("@stack-guid", stackGuid(appName)),
					replace("@route-guid", routeGuid(appName))))));
	}

	public void stubPushApp(final String appName, ContentPattern<?>... appMetadataPatterns) {
		stubCreateAppMetadata(appName, appMetadataPatterns);
		stubAppAfterCreation(appName, appName);
	}

	public void stubPushAppWithHost(final String appName, final String host, ContentPattern<?>... appMetadataPatterns) {
		stubCreateAppMetadata(appName, appMetadataPatterns);
		stubAppAfterCreation(appName, host);
	}

	public void stubUpdateAppWithUpgrade(final String appName) {
		stubUpdateEnvironment(appName);
		stubCreatePackage(appName);
		stubCreateBuild(appName);
		stubCreateDeployment(appName);
	}

	public void stubUpdateAppWithHostAndDomain(final String appName) {
		stubUpdateApp(appName);
		stubCreateRoute(appName);
	}

	public void stubUpdateApp(final String appName) {
		stubUpdateEnvironment(appName);
		stubGetPackage(appName);
		stubCreateBuild(appName);
		stubCreateDeployment(appName);
	}

	private void stubAppAfterCreation(String appName, String host) {
		stubMapRouteToApp(appName, host);
		stubUploadAppBits(appName);
		stubInitializeAppState(appName);
		stubCheckAppState(appName);
	}

	private void stubUpdateEnvironment(String appName) {
		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.environment_json)]"))
			.willReturn(ok()
				.withBody(cc("get-app-STARTED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubCreatePackage(String appName) {
		stubFor(post(urlPathEqualTo("/v3/packages"))
			.withRequestBody(
				matchingJsonPath("$.[?(@.relationships.app.data.guid == '" + appGuid(appName) + "')]"))
			.willReturn(ok()
				.withBody(cc("get-package-READY",
					replace("@guid", packageGuid(appName)),
					replace("@app-guid", appGuid(appName))))));

		stubFor(post(urlPathEqualTo("/v3/packages/" + packageGuid(appName) + "/upload"))
			.willReturn(ok()
				.withBody(cc("get-package-READY",
					replace("@guid", packageGuid(appName)),
					replace("@app-guid", appGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v3/packages/" + packageGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-package-READY",
					replace("@guid", packageGuid(appName)),
					replace("@app-guid", appGuid(appName))))));
	}

	private void stubGetPackage(String appName) {
		stubFor(get(urlPathEqualTo("/v3/apps/" + appGuid(appName) + "/packages"))
			.withQueryParam("states", equalTo("READY"))
			.willReturn(ok()
				.withBody(cc("get-packages-READY",
					replace("@package-guid", packageGuid(appName))))));
	}

	private void stubCreateBuild(String appName) {
		stubFor(post(urlPathEqualTo("/v3/builds"))
			.withRequestBody(
				matchingJsonPath("$.[?(@.package.guid == '" + packageGuid(appName) + "')]"))
			.willReturn(ok()
				.withBody(cc("get-build-STAGED",
					replace("@guid", buildGuid(appName)),
					replace("@package-guid", packageGuid(appName)),
					replace("@app-guid", appGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v3/builds/" + buildGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-build-STAGED",
					replace("@guid", buildGuid(appName)),
					replace("@package-guid", packageGuid(appName)),
					replace("@app-guid", appGuid(appName))))));
	}

	private void stubCreateDeployment(String appName) {
		stubFor(post(urlPathEqualTo("/v3/deployments"))
			.withRequestBody(
				matchingJsonPath("$.[?(@.relationships.app.data.guid == '" + appGuid(appName) + "')]"))
			.willReturn(ok()
				.withBody(cc("get-deployment-DEPLOYED",
					replace("@guid", deploymentGuid(appName)),
					replace("@app-guid", appGuid(appName))))));

		stubFor(get(urlPathEqualTo("/v3/deployments/" + deploymentGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-deployment-DEPLOYED",
					replace("@guid", deploymentGuid(appName)),
					replace("@app-guid", appGuid(appName))))));
	}

	private void stubCreateAppMetadata(String appName, ContentPattern<?>... appMetadataPatterns) {
		MappingBuilder mappingBuilder = post(urlPathEqualTo("/v2/apps"))
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

	private void stubMapRouteToApp(String appName, String host) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/routes"))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlPathEqualTo("/v2/routes"))
			.withQueryParam("q", equalTo("domain_guid:" + TEST_ORG_GUID))
			.withQueryParam("q", equalTo("host:" + host))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(post(urlPathEqualTo("/v2/routes"))
			.withRequestBody(matchingJsonPath("$.[?(@.host == '" + host + "')]"))
			.willReturn(created()
				.withBody(cc("list-routes",
					replace("@name", appName),
					replace("@guid", routeGuid(appName))))));

		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/routes/" + routeGuid(appName)))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubUploadAppBits(String appName) {
		stubFor(put(urlPathEqualTo("/v2/resource_match"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody("[]")));

		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/bits"))
			.withQueryParam("async", equalTo("true"))
			.willReturn(created()
				.withBody(cc("put-app-bits",
					replace("@guid", appName + "-JOB-GUID")))));
	}

	private void stubInitializeAppState(String appName) {
		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STOPPED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));

		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STARTED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STARTED",
					replace("@name", appName),
					replace("@guid", appGuid(appName))))));
	}

	private void stubCheckAppState(String appName) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-app-STAGED",
					replace("@name", appName)))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/instances"))
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));
	}

	private void stubCreateRoute(final String appName) {
		stubFor(get(urlPathEqualTo("/v2/private_domains"))
			.withQueryParam("page", equalTo("1"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/services"))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("list-space-services")
					.replace("@service-guid", "SERVICE-ID")
					.replace("@service-name", "db-service"))));

		stubFor(post(urlPathEqualTo("/v2/routes"))
			.willReturn(ok()
				.withBody(cc("post-route")
					.replace("@guid", routeGuid(appName)))));

		stubFor(put(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/routes/" + routeGuid(appName)))
			.willReturn(ok()
				.withBody(cc("get-app-summary"))));
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
		stubServiceInstanceExists(serviceInstanceName, "doNotCare", "doNotCare");
	}

	public void stubServiceInstanceExists(String serviceInstanceName, String serviceName, String planName) {
		stubServiceInstanceExists(serviceInstanceGuid(serviceInstanceName), serviceInstanceName, serviceName, planName);
	}

	private void stubServiceInstanceExists(String serviceInstanceId, String serviceInstanceName, String serviceName,
		String planName) {
		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/service_instances"))
			.withQueryParam("q", equalTo("name:" + serviceInstanceName))
			.withQueryParam("page", equalTo("1"))
			.withQueryParam("return_user_provided_service_instances", equalTo("true"))
			.willReturn(ok()
				.withBody(cc("list-space-service_instances",
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@service-guid", serviceGuid(serviceName)),
					replace("@plan-guid", planGuid(planName)),
					replace("@name", serviceInstanceName),
					replace("@guid", serviceInstanceId)))));
	}

	public void stubServiceBindingsDoNotExist(String serviceInstanceName) {
		stubFor(get(urlPathEqualTo("/v2/service_bindings"))
			.withQueryParam("q", equalTo("service_instance_guid:" + serviceInstanceGuid(serviceInstanceName)))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	public void stubGetBackingServiceInstance(String serviceInstanceName, String serviceName, String planName) {
		String serviceInstanceId = serviceInstanceGuid(serviceInstanceName);
		stubServiceInstanceExists(serviceInstanceId, serviceInstanceName, serviceName, planName);
		stubGetServiceAndGetPlan(serviceName, planName);
	}

	public void stubServiceExists(String serviceName, String planName) {
		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/services"))
			.withQueryParam("q", equalTo("label:" + serviceName))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("list-space-services")
					.replace("@service-guid", serviceGuid(serviceName))
					.replace("@plan-guid", planGuid(planName))
					.replace("@service-name", serviceName))));

		stubFor(get(urlPathEqualTo("/v2/service_plans"))
			.withQueryParam("q", equalTo("service_guid:" + serviceGuid(serviceName)))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("list-service-plans")
					.replace("@service-guid", serviceGuid(serviceName))
					.replace("@plan-name", planName)
					.replace("@plan-guid", planGuid(planName)))));
	}

	public void stubCreateServiceInstance(String serviceInstanceName) {
		stubFor(post(urlPathEqualTo("/v2/service_instances"))
			.withQueryParam("accepts_incomplete", equalTo("true"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + serviceInstanceName + "')]"))
			.willReturn(ok()));
	}

	public void stubCreateServiceInstanceWithParameters(String serviceInstanceName, Map<String, Object> params) {
		stubFor(post(urlPathEqualTo("/v2/service_instances"))
			.withQueryParam("accepts_incomplete", equalTo("true"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + serviceInstanceName + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.parameters == " + new JSONObject(params) + ")]"))
			.willReturn(ok()));
	}

	public void stubUpdateServiceInstanceWithParameters(String serviceInstanceName, Map<String, Object> params) {
		stubFor(put(urlPathEqualTo("/v2/service_instances/" + serviceInstanceGuid(serviceInstanceName)))
			.withQueryParam("accepts_incomplete", equalTo("true"))
			.withRequestBody(matchingJsonPath("$.[?(@.parameters == " + new JSONObject(params) + ")]"))
			.willReturn(ok()));
	}

	public void stubDeleteServiceInstance(String serviceInstanceName) {
		String serviceInstanceGuid = serviceInstanceGuid(serviceInstanceName);

		stubFor(delete(urlPathEqualTo("/v2/service_instances/" + serviceInstanceGuid))
			.willReturn(ok()));
	}

	public void stubCreateServiceBinding(String appName, String serviceInstanceName) {
		String serviceInstanceGuid = serviceInstanceGuid(serviceInstanceName);
		String serviceBindingGuid = serviceBindingGuid(appName, serviceInstanceName);

		stubFor(post(urlPathEqualTo("/v2/service_bindings"))
			.withRequestBody(matchingJsonPath("$.[?(@.app_guid == '" + appGuid(appName) + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.service_instance_guid == '" + serviceInstanceGuid + "')]"))
			.willReturn(created()
				.withBody(cc("get-service_binding",
					replace("@guid", serviceBindingGuid),
					replace("@app-guid", appGuid(appName)),
					replace("@service-instance-guid", serviceInstanceGuid)))));
	}

	public void stubDeleteServiceBinding(String appName, String serviceInstanceName) {
		String serviceBindingGuid = serviceBindingGuid(appName, serviceInstanceName);

		stubFor(delete(urlPathEqualTo("/v2/service_bindings/" + serviceBindingGuid))
			.withQueryParam("async", equalTo("true"))
			.willReturn(noContent()));
	}

	public void stubServiceBindingExists(String appName, String serviceInstanceName) {
		String serviceInstanceGuid = serviceInstanceGuid(serviceInstanceName);
		String serviceBindingGuid = serviceBindingGuid(appName, serviceInstanceName);

		stubFor(get(urlPathEqualTo("/v2/service_bindings"))
			.withQueryParam("q", equalTo("service_instance_guid:" + serviceInstanceGuid))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("list-service-bindings",
					replace("@guid", serviceBindingGuid),
					replace("@app-guid", appGuid(appName)),
					replace("@service-instance-guid", serviceInstanceGuid)))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/service_bindings"))
			.withQueryParam("q", equalTo("service_instance_guid:" + serviceInstanceGuid))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc("get-service_bindings",
					replace("@service-binding-guid", serviceBindingGuid),
					replace("@app-guid", appGuid(appName)),
					replace("@service-instance-guid", serviceInstanceGuid)))));
	}

	public void stubServiceBindingDoesNotExist(String appName) {
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid(appName) + "/service_bindings"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	public void stubAssociatePermissions(final String spaceName) {
		stubFor(get(urlPathEqualTo("/v2/config/feature_flags/set_roles_by_username"))
			.willReturn(ok()
				.withBody(cc("get-feature-flag-roles"))));
		stubSpaceExists(spaceName);

		stubFor(put(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/users"))
			.willReturn(ok()));
	}

	private void stubGetServiceAndGetPlan(String serviceName, String planName) {
		stubFor(get(urlPathEqualTo("/v2/service_plans/" + planGuid(planName)))
			.willReturn(ok()
				.withBody(cc("get-service-plan")
					.replace("@service-guid", serviceGuid(serviceName))
					.replace("@plan-name", planName)
					.replace("@plan-guid", planGuid(planName)))));

		stubFor(get(urlPathEqualTo("/v2/services/" + serviceGuid(serviceName)))
			.willReturn(ok()
				.withBody(cc("get-service")
					.replace("@service-guid", serviceGuid(serviceName))
					.replace("@service-name", serviceName))));
	}

	private void stubSpaceExists(final String spaceName) {
		stubFor(get(urlPathEqualTo("/v2/organizations/" + TEST_ORG_GUID + "/spaces"))
			.withQueryParam("q", equalTo("name:" + spaceName))
			.willReturn(ok()
				.withBody(cc("list-spaces",
					replace("@org-guid", TEST_ORG_GUID),
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@name", spaceName)))));
	}

	private String cc(String fileRoot, StringReplacementPair... replacements) {
		String response = readResponseFromFile(fileRoot, "cloudcontroller");
		for (StringReplacementPair pair : replacements) {
			response = response.replaceAll(pair.getRegex(), pair.getReplacement());
		}
		return response;
	}

	private static String appGuid(String appName) {
		return appName + "-GUID";
	}

	private static String routeGuid(String appName) {
		return appName + "-ROUTE-GUID";
	}

	private static String stackGuid(String appName) {
		return appName + "-STACK-GUID";
	}

	public static String serviceGuid(String serviceName) {
		return serviceName + "-SERVICE-GUID";
	}

	public static String planGuid(String planName) {
		return planName + "-PLAN-GUID";
	}

	private static String serviceInstanceGuid(String serviceInstanceName) {
		return serviceInstanceName + "-INSTANCE-GUID";
	}

	private static String serviceBindingGuid(String appName, String serviceInstanceName) {
		return appGuid(appName) + "-" + serviceInstanceGuid(serviceInstanceName);
	}

	private static String packageGuid(String appName) {
		return appName + "-PACKAGE-GUID";
	}

	private static String buildGuid(String appName) {
		return appName + "-BUILD-GUID";
	}

	private static String deploymentGuid(String appname) {
		return appname + "-DEPLOYMENT-GUID";
	}

}
