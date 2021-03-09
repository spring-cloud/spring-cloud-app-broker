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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesResponse;
import org.cloudfoundry.client.v2.spaces.DeleteSpaceRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.organizations.OrganizationDetail;
import org.cloudfoundry.operations.organizations.OrganizationInfoRequest;
import org.cloudfoundry.operations.organizations.OrganizationQuota;
import org.cloudfoundry.operations.organizations.Organizations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeleteBackingSpaceRequest;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CloudFoundryAppDeployerDeleteSpaceTest {

	private AppDeployer appDeployer;

	@Mock
	private Organizations operationsOrganizations;

	@Mock
	private org.cloudfoundry.client.v2.spaces.Spaces clientSpaces;

	@Mock
	private org.cloudfoundry.client.v2.organizations.Organizations clientOrganizations;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@Mock
	private CloudFoundryClient cloudFoundryClient;

	@Mock
	private CloudFoundryOperationsUtils operationsUtils;

	@Mock
	private ResourceLoader resourceLoader;

	@BeforeEach
	void setUp() {
		CloudFoundryDeploymentProperties deploymentProperties = new CloudFoundryDeploymentProperties();

		CloudFoundryTargetProperties targetProperties = new CloudFoundryTargetProperties();
		targetProperties.setDefaultOrg("default-org");
		targetProperties.setDefaultSpace("default-space");

		given(cloudFoundryOperations.organizations()).willReturn(operationsOrganizations);
		given(cloudFoundryClient.organizations()).willReturn(clientOrganizations);

		given(operationsOrganizations
			.get(
				OrganizationInfoRequest
					.builder()
					.name("default-org")
					.build()))
			.willReturn(Mono.just(
				OrganizationDetail
					.builder()
					.id("default-org-id")
					.name("default-org")
					.quota(OrganizationQuota
						.builder()
						.id("quota-id")
						.instanceMemoryLimit(0)
						.organizationId("default-org-id")
						.name("quota")
						.paidServicePlans(false)
						.totalMemoryLimit(0)
						.totalRoutes(0)
						.totalServiceInstances(0)
						.build())
					.build()));

		appDeployer = new CloudFoundryAppDeployer(deploymentProperties, cloudFoundryOperations, cloudFoundryClient,
			operationsUtils, targetProperties, resourceLoader);
	}

	@Test
	void deleteSpaceIfExists() {
		given(clientOrganizations
			.listSpaces(ListOrganizationSpacesRequest
				.builder()
				.name("test-space-name")
				.organizationId("default-org-id")
				.page(1)
				.build()))
			.willReturn(Mono.just(ListOrganizationSpacesResponse
				.builder()
				.resource(SpaceResource
					.builder()
					.entity(SpaceEntity
						.builder()
						.name("test-space-name")
						.build())
					.metadata(Metadata
						.builder()
						.id("test-space-id")
						.build())
					.build())
				.build()));

		given(cloudFoundryClient.spaces()).willReturn(clientSpaces);
		given(clientSpaces
			.delete(DeleteSpaceRequest
				.builder()
				.spaceId("test-space-id")
				.recursive(true)
				.build()))
			.willReturn(Mono.empty());

		DeleteBackingSpaceRequest request =
			DeleteBackingSpaceRequest.builder()
				.name("test-space-name")
				.build();

		StepVerifier.create(
			appDeployer.deleteBackingSpace(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("test-space-name"))
			.verifyComplete();
	}

	@Test
	void doNothingIfSpaceDoesNotExist() {
		given(clientOrganizations
			.listSpaces(ListOrganizationSpacesRequest
				.builder()
				.name("test-space-name")
				.organizationId("default-org-id")
				.page(1)
				.build()))
			.willReturn(Mono.just(ListOrganizationSpacesResponse
				.builder()
				.resources()
				.build()));

		DeleteBackingSpaceRequest request =
			DeleteBackingSpaceRequest.builder()
				.name("test-space-name")
				.build();

		StepVerifier.create(
			appDeployer.deleteBackingSpace(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo("test-space-name"))
			.verifyComplete();
	}

}
