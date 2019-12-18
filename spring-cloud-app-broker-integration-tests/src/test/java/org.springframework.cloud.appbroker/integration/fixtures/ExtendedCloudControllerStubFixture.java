package org.springframework.cloud.appbroker.integration.fixtures;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public class ExtendedCloudControllerStubFixture extends CloudControllerStubFixture {

	public void stubSpaceServiceWithResponse(String responseFile) {
		stubFor(get(urlPathEqualTo("/v2/spaces/" + TEST_SPACE_GUID + "/services"))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc(responseFile))));
	}

	public void stubServicePlanWithResponse(String responseFile) {
		stubServicePlanWithResponse(responseFile, "SERVICE-ID");
	}

	public void stubServicePlanWithResponse(String responseFile, String serviceGuid) {
		stubFor(get(urlPathEqualTo("/v2/service_plans"))
			.withQueryParam("q", equalTo("service_guid:"+ serviceGuid))
			.withQueryParam("page", equalTo("1"))
			.willReturn(ok()
				.withBody(cc(responseFile))));
	}

}
