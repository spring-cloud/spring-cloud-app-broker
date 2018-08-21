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

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
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
	}

	public void stubAppDoesNotExist(final String appName) {
		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps?q=name:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));
	}

	public void stubAppExists(final String appName) {
		String appGuid = appName + "-GUID";
		String stackGuid = appName + "-STACK-GUID";
		String routeGuid = appName + "-ROUTE-GUID";

		stubFor(get(urlEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/apps?q=name:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("list-space-apps",
					replace("@guid", appGuid),
					replace("@space-guid", TEST_SPACE_GUID),
					replace("@stack-guid", stackGuid)))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid + "/instances"))
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid + "/summary"))
			.willReturn(ok()
				.withBody(cc("get-app-summary",
					replace("@name", appName),
					replace("@guid", appGuid),
					replace("@stack-guid", stackGuid),
					replace("@route-guid", routeGuid)))));

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid + "/stats"))
			.willReturn(ok()
				.withBody(cc("get-app-stats",
					replace("@name", appName)))));

		stubFor(get(urlPathEqualTo("/v2/stacks/" + stackGuid))
			.willReturn(ok()
				.withBody(cc("get-stack",
					replace("@guid", stackGuid)))));
	}

	public void stubPushApp(final String appName, ContentPattern<?>... appMetadataPatterns) {
		String appGuid = appName + "-GUID";
		String routeGuid = appName + "-ROUTE-GUID";
		String uploadBitsJogGuid = appName + "-JOB-GUID";

		//
		// create app metadata
		//
		MappingBuilder mappingBuilder = post(urlEqualTo("/v2/apps"))
			.withRequestBody(matchingJsonPath("$.[?(@.name == '" + appName + "')]"));
		for (ContentPattern<?> appMetadataPattern : appMetadataPatterns) {
			mappingBuilder.withRequestBody(appMetadataPattern);
		}
		stubFor(mappingBuilder
			.willReturn(ok()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid)))));

		//
		// map route to app
		//
		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid + "/routes"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(get(urlEqualTo("/v2/routes?q=domain_guid:" + TEST_ORG_GUID + "&q=host:" + appName + "&page=1"))
			.willReturn(ok()
				.withBody(cc("empty-query-results"))));

		stubFor(post(urlEqualTo("/v2/routes"))
			.withRequestBody(matchingJsonPath("$.[?(@.host == '" + appName + "')]"))
			.willReturn(created()
				.withBody(cc("list-routes",
					replace("@name", appName),
					replace("@guid", routeGuid)))));

		stubFor(put(urlEqualTo("/v2/apps/" + appGuid + "/routes/" + routeGuid))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid)))));

		//
		// perform pre-upload resource matching
		//
		stubFor(put(urlEqualTo("/v2/resource_match"))
			.withMetadata(optionalStubMapping())
			.willReturn(ok()
				.withBody("[]")));

		//
		// upload app bits
		//
		stubFor(put(urlEqualTo("/v2/apps/" + appGuid + "/bits?async=true"))
			.willReturn(created()
				.withBody(cc("put-app-bits",
					replace("@guid", uploadBitsJogGuid)))));

		//
		// initialize app state
		//
		stubFor(put(urlEqualTo("/v2/apps/" + appGuid))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STOPPED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STOPPED",
					replace("@name", appName),
					replace("@guid", appGuid)))));

		stubFor(put(urlEqualTo("/v2/apps/" + appGuid))
			.withRequestBody(matchingJsonPath("$.[?(@.state == 'STARTED')]"))
			.willReturn(created()
				.withBody(cc("get-app-STARTED",
					replace("@name", appName),
					replace("@guid", appGuid)))));

		//
		// poll for app ready
		//
		stubFor(get(urlEqualTo("/v2/apps/" + appGuid))
			.willReturn(ok()
				.withBody(cc("get-app-STAGED"))));

		stubFor(get(urlEqualTo("/v2/apps/" + appGuid + "/instances"))
			.willReturn(ok()
				.withBody(cc("get-app-instances"))));
	}

	public void stubDeleteApp(String appName) {
		String appGuid = appName + "-GUID";
		String routeGuid = appName + "-ROUTE-GUID";

		stubFor(delete(urlPathEqualTo("/v2/routes/" + appName + "-ROUTE-GUID"))
			.willReturn(ok()
				.withBody(cc("get-route",
					replace("@guid", routeGuid)))));

		stubFor(delete(urlPathEqualTo("/v2/apps/" + appGuid))
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

	public void stubCreateServiceBinding(String appName, String serviceInstanceName) {
		String appGuid = appName + "-GUID";
		String serviceInstanceGuid = serviceInstanceName + "-GUID";
		String serviceBindingGuid = appGuid + "-" + serviceInstanceGuid;

		stubFor(post(urlEqualTo("/v2/service_bindings"))
			.withRequestBody(matchingJsonPath("$.[?(@.app_guid == '" + appGuid + "')]"))
			.withRequestBody(matchingJsonPath("$.[?(@.service_instance_guid == '" + serviceInstanceGuid + "')]"))
			.willReturn(created()
				.withBody(cc("list-service-bindings",
					replace("@guid", serviceBindingGuid),
					replace("@app-guid", appGuid),
					replace("@service-instance-guid", serviceInstanceGuid)))));
	}

	public void stubServiceBindingDoesNotExist(String appName) {
		String appGuid = appName + "-GUID";

		stubFor(get(urlPathEqualTo("/v2/apps/" + appGuid + "/service_bindings"))
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
}
