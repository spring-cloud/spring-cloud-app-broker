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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.Routes;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.applications.ApplicationsV3;
import org.cloudfoundry.client.v3.applications.ListApplicationPackagesResponse;
import org.cloudfoundry.client.v3.builds.BuildState;
import org.cloudfoundry.client.v3.builds.Builds;
import org.cloudfoundry.client.v3.builds.CreateBuildResponse;
import org.cloudfoundry.client.v3.builds.CreatedBy;
import org.cloudfoundry.client.v3.builds.Droplet;
import org.cloudfoundry.client.v3.builds.GetBuildResponse;
import org.cloudfoundry.client.v3.deployments.CreateDeploymentResponse;
import org.cloudfoundry.client.v3.deployments.DeploymentState;
import org.cloudfoundry.client.v3.deployments.DeploymentsV3;
import org.cloudfoundry.client.v3.deployments.GetDeploymentResponse;
import org.cloudfoundry.client.v3.packages.BitsData;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageResponse;
import org.cloudfoundry.client.v3.packages.PackageResource;
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.Packages;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.Applications;
import org.cloudfoundry.operations.domains.Domain;
import org.cloudfoundry.operations.domains.Domains;
import org.cloudfoundry.operations.domains.Status;
import org.cloudfoundry.operations.services.BindServiceInstanceRequest;
import org.cloudfoundry.operations.services.Services;
import org.cloudfoundry.operations.spaces.SpaceDetail;
import org.cloudfoundry.operations.spaces.Spaces;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.cloud.appbroker.deployer.DeploymentProperties.TARGET_PROPERTY_KEY;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudFoundryAppDeployerUpdateApplicationTest {

	private static final String APP_ID = "app-id";

	private static final String APP_NAME = "test-app";

	private static final String APP_PATH = "test.jar";

	private AppDeployer appDeployer;

	@Mock
	private Applications operationsApplications;

	@Mock
	private Services operationsServices;

	@Mock
	private ApplicationsV2 applicationsV2;

	@Mock
	private ApplicationsV3 applicationsV3;

	@Mock
	private Builds builds;

	@Mock
	private DeploymentsV3 deploymentsV3;

	@Mock
	private Domains domains;

	@Mock
	private Packages packages;

	@Mock
	private Routes routes;

	@Mock
	private Spaces spaces;

	@Mock
	private CloudFoundryOperations cloudFoundryOperations;

	@Mock
	private CloudFoundryClient cloudFoundryClient;

	@Mock
	private CloudFoundryOperationsUtils operationsUtils;

	@Mock
	private ResourceLoader resourceLoader;

	@BeforeEach
	@SuppressWarnings({"PMD.ExcessiveMethodLength", "deprecation"})
	void setUp() {
		CloudFoundryDeploymentProperties deploymentProperties = new CloudFoundryDeploymentProperties();
		CloudFoundryTargetProperties targetProperties = new CloudFoundryTargetProperties();
		targetProperties.setDefaultSpace("default-space");

		given(operationsApplications.pushManifest(any())).willReturn(Mono.empty());
		given(resourceLoader.getResource(APP_PATH)).willReturn(new FileSystemResource(APP_PATH));

		given(cloudFoundryOperations.services()).willReturn(operationsServices);
		given(cloudFoundryOperations.applications()).willReturn(operationsApplications);
		given(cloudFoundryOperations.domains()).willReturn(domains);
		given(cloudFoundryOperations.spaces()).willReturn(spaces);
		given(cloudFoundryClient.applicationsV2()).willReturn(applicationsV2);
		given(cloudFoundryClient.applicationsV3()).willReturn(applicationsV3);
		given(cloudFoundryClient.packages()).willReturn(packages);
		given(cloudFoundryClient.builds()).willReturn(builds);
		given(cloudFoundryClient.deploymentsV3()).willReturn(deploymentsV3);
		given(cloudFoundryClient.routes()).willReturn(routes);
		given(operationsUtils.getOperations(anyMap())).willReturn(Mono.just(cloudFoundryOperations));
		given(operationsUtils.getOperationsForSpace(anyString())).willReturn(Mono.just(cloudFoundryOperations));

		appDeployer = new CloudFoundryAppDeployer(deploymentProperties,
			cloudFoundryOperations, cloudFoundryClient, operationsUtils, targetProperties, resourceLoader);

		given(operationsApplications.get(any()))
			.willReturn(Mono.just(createApplicationDetail()));

		given(applicationsV2.summary(any(SummaryApplicationRequest.class)))
			.willReturn(Mono.just(SummaryApplicationResponse.builder()
				.id(APP_ID)
				.name(APP_NAME)
				.services(Collections.emptyList())
				.build()));

		given(packages.create(any()))
			.willReturn(Mono.just(CreatePackageResponse.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("DATETIME")
				.id("package-id")
				.build()));

		given(packages.upload(any()))
			.willReturn(Mono.just(UploadPackageResponse.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("2019-07-05T10:37:47Z")
				.createdAt("2019-07-05T10:37:47Z")
				.id("package-id")
				.build()));


		given(packages.get(any()))
			.willReturn(Mono.just(GetPackageResponse.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("DATETIME")
				.id("package-id")
				.build()));

		given(applicationsV3.listPackages(any()))
			.willReturn(Mono.just(ListApplicationPackagesResponse.builder()
				.resource(createPackage("2019-07-05T10:37:47Z", "package-id-1"))
				.resource(createPackage("2019-07-05T12:37:47Z", "package-id-2"))
				.resource(createPackage("2019-07-06T10:37:47Z", "package-id-3"))
				.resource(createPackage("2019-07-06T11:37:47Z", "package-id-4"))
				.build()));

		given(builds.create(any()))
			.willReturn(Mono.just(CreateBuildResponse.builder()
				.state(BuildState.STAGING)
				.createdBy(CreatedBy.builder().id("create-by-id").email("an-email").name("creator").build())
				.inputPackage(Relationship.builder().id("package-id").build())
				.lifecycle(createLifecycle())
				.createdAt("DATETIME")
				.id("build-id")
				.build()));
		given(builds.get(any()))
			.willReturn(Mono.just(GetBuildResponse.builder()
				.state(BuildState.STAGED)
				.createdBy(CreatedBy.builder().id("create-by-id").email("an-email").name("creator").build())
				.inputPackage(Relationship.builder().id("package-id").build())
				.lifecycle(createLifecycle())
				.droplet(Droplet.builder().id("droplet-id").build())
				.createdAt("DATETIME")
				.id("build-id")
				.build()));

		given(deploymentsV3.create(any()))
			.willReturn(Mono.just(CreateDeploymentResponse.builder()
				.state(DeploymentState.DEPLOYED)
				.createdAt("DATETIME")
				.id("deployment-id")
				.build()));
		given(deploymentsV3.get(any()))
			.willReturn(Mono.just(GetDeploymentResponse.builder()
				.state(DeploymentState.DEPLOYED)
				.createdAt("DATETIME")
				.id("deployment-id")
				.build()));
	}

	@Test
	void updateApp() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();
	}

	@Test
	void updateAppWithTarget() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		Map<String, String> properties = singletonMap(TARGET_PROPERTY_KEY, "service-instance-id");
		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(operationsUtils).should().getOperations(properties);
		then(operationsUtils).shouldHaveNoMoreInteractions();
	}

	@Test
	void updateAppProperties() {
		ArgumentCaptor<org.cloudfoundry.client.v2.applications.UpdateApplicationRequest> updateApplicationRequestCaptor =
			ArgumentCaptor.forClass(org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.class);

		given(applicationsV2.update(updateApplicationRequestCaptor.capture()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		Map<String, String> properties = new HashMap<>();
		properties.put("count", "2");
		properties.put("disk", "2G");
		properties.put("memory", "1G");

		UpdateApplicationRequest request =
			UpdateApplicationRequest
				.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.properties(properties)
				.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		org.cloudfoundry.client.v2.applications.UpdateApplicationRequest updateApplicationRequest = updateApplicationRequestCaptor
			.getValue();
		assertThat(updateApplicationRequest.getInstances()).isEqualTo(2);
		assertThat(updateApplicationRequest.getDiskQuota()).isEqualTo(2048);
		assertThat(updateApplicationRequest.getMemory()).isEqualTo(1024);
	}

	@Test
	void updateAppWithUpgrade() {
		Map<String, String> properties = new HashMap<>();
		properties.put("upgrade", "true");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(applicationsV2).should().summary(any(SummaryApplicationRequest.class));
		then(applicationsV2).shouldHaveNoMoreInteractions();
	}

	@Test
	void updateAppWithNoNewServices() {
		given(applicationsV2.summary(any(SummaryApplicationRequest.class)))
			.willReturn(Mono.just(SummaryApplicationResponse.builder()
				.id(APP_ID)
				.name(APP_NAME)
				.service(ServiceInstance.builder()
					.name("service-1")
					.build())
				.service(ServiceInstance.builder()
					.name("service-2")
					.build())
				.build()));
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.service("service-1")
			.service("service-2")
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(applicationsV2).should().summary(SummaryApplicationRequest.builder().applicationId(APP_ID).build());
		then(applicationsV2).should().update(argThat(arg -> arg.getApplicationId().equals(APP_ID)));
		then(operationsServices).shouldHaveNoMoreInteractions();
		then(applicationsV2).shouldHaveNoMoreInteractions();
	}

	@Test
	void updateAppWithNewServices() {
		given(operationsServices.bind(any(BindServiceInstanceRequest.class)))
			.willReturn(Mono.empty());
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.service("service-1")
			.service("service-2")
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(applicationsV2).should().summary(SummaryApplicationRequest.builder().applicationId(APP_ID).build());
		then(operationsServices).should()
			.bind(BindServiceInstanceRequest.builder().serviceInstanceName("service-1").applicationName(APP_NAME)
				.build());
		then(operationsServices).should()
			.bind(BindServiceInstanceRequest.builder().serviceInstanceName("service-2").applicationName(APP_NAME)
				.build());
		then(applicationsV2).should().update(argThat(arg -> arg.getApplicationId().equals(APP_ID)));

		then(operationsServices).shouldHaveNoMoreInteractions();
		then(applicationsV2).shouldHaveNoMoreInteractions();
	}

	@Test
	void updateAppWithHostAndDomain() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		given(domains.list()).willReturn(getDomains());

		given(spaces.get(any())).willReturn(
			Mono.just(
				SpaceDetail.builder()
					.id("space-id")
					.name("space-name")
					.organization("org-name")
					.build()));
		given(routes.create(any()))
			.willReturn(Mono.just(CreateRouteResponse.builder()
				.metadata(Metadata.builder()
					.id("route-id")
					.build()).build()));
		given(applicationsV2.associateRoute(any()))
			.willReturn(Mono.empty());

		Map<String, String> properties = new HashMap<>();
		properties.put("host", "my.host");
		properties.put("domain", "my.domain.com");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();


		then(routes).should().create(CreateRouteRequest.builder()
			.domainId("myDomainComId")
			.spaceId("space-id")
			.host("my.host")
			.build());
		then(applicationsV2).should().associateRoute(AssociateApplicationRouteRequest.builder()
			.applicationId(APP_ID)
			.routeId("route-id")
			.build());
	}

	@Test
	void updateAppWithHostAndMergedDomains() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		given(spaces.get(any())).willReturn(Mono.just(SpaceDetail.builder()
			.id("space-id")
			.name("space-name")
			.organization("org-name")
			.build()));

		mockDomainsAndRoutes();

		Map<String, String> properties = new HashMap<>();
		properties.put("host", "my.host");
		properties.put("domain", "my.domain.com");
		properties.put("domains", "my.domain.internal.com,my.domain.default.com");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		verifyRouteCreatedAndMapped("myDomainComId", "my.host", "space-id", APP_ID);
		verifyRouteCreatedAndMapped("myDomainInternalId", "my.host", "space-id", APP_ID);
		verifyRouteCreatedAndMapped("myDomainDefaultId", "my.host", "space-id", APP_ID);
	}

	@Test
	void updateAppWithHostAndDomains() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		given(spaces.get(any())).willReturn(Mono.just(SpaceDetail.builder()
			.id("space-id")
			.name("space-name")
			.organization("org-name")
			.build()));

		mockDomainsAndRoutes();

		Map<String, String> properties = new HashMap<>();
		properties.put("host", "my.host");
		properties.put("domains", "my.domain.internal.com,my.domain.default.com");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		verifyRouteCreatedAndMapped("myDomainInternalId", "my.host", "space-id", APP_ID);
		verifyRouteCreatedAndMapped("myDomainDefaultId", "my.host", "space-id", APP_ID);
	}

	@Test
	void updateAppWithHostAndNoDomain() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		given(domains.list())
			.willReturn(getDomains());

		given(spaces.get(any()))
			.willReturn(Mono.just(SpaceDetail.builder()
				.id("space-id")
				.name("space-name")
				.organization("org-name")
				.build()));
		given(routes.create(any()))
			.willReturn(Mono.just(CreateRouteResponse.builder()
				.metadata(Metadata
					.builder()
					.id("route-id")
					.build())
				.build()));
		given(applicationsV2.associateRoute(any()))
			.willReturn(Mono.empty());

		Map<String, String> properties = new HashMap<>();
		properties.put("host", "my.host");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();

		then(routes).should().create(CreateRouteRequest.builder()
			.domainId("myDomainDefaultId")
			.spaceId("space-id")
			.host("my.host")
			.build());
		then(applicationsV2).should().associateRoute(AssociateApplicationRouteRequest.builder()
			.applicationId(APP_ID)
			.routeId("route-id")
			.build());
	}

	@Test
	void updateAppWithDomainAndNoHost() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(
				UpdateApplicationResponse.builder().build()));

		given(domains.list())
			.willReturn(getDomains());

		given(spaces.get(any()))
			.willReturn(Mono.just(SpaceDetail.builder()
				.id("space-id")
				.name("space-name")
				.organization("org-name")
				.build()));
		given(routes.create(any()))
			.willReturn(Mono.just(CreateRouteResponse.builder()
				.metadata(Metadata
					.builder()
					.id("route-id")
					.build())
				.build()));
		given(applicationsV2.associateRoute(any()))
			.willReturn(Mono.empty());

		Map<String, String> properties = new HashMap<>();
		properties.put("domain", "my.domain.com");

		UpdateApplicationRequest request = UpdateApplicationRequest.builder()
			.name(APP_NAME)
			.path(APP_PATH)
			.properties(properties)
			.build();

		StepVerifier.create(appDeployer.update(request))
			.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
			.verifyComplete();


		then(routes).should().create(CreateRouteRequest.builder()
			.domainId("myDomainComId")
			.spaceId("space-id")
			.host(null)
			.build());
		then(applicationsV2).should().associateRoute(AssociateApplicationRouteRequest
			.builder()
			.applicationId(APP_ID)
			.routeId("route-id")
			.build());
	}

	@Test
	void updateAppWithNotExistingDomain() {
		given(applicationsV2.update(any()))
			.willReturn(Mono.just(UpdateApplicationResponse.builder()
				.build()));

		given(domains.list())
			.willReturn(getDomains());

		given(spaces.get(any()))
			.willReturn(Mono.just(SpaceDetail.builder()
				.id("space-id")
				.name("space-name")
				.organization("org-name")
				.build()));

		Map<String, String> properties = new HashMap<>();
		properties.put("host", "my.host");
		properties.put("domain", "non.existing.domain.com");

		UpdateApplicationRequest request =
			UpdateApplicationRequest
				.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.properties(properties)
				.build();

		StepVerifier.create(appDeployer.update(request))
			.expectError(RuntimeException.class)
			.verify();

		then(routes).shouldHaveNoInteractions();
	}

	private Flux<Domain> getDomains() {
		return Flux.just(
			Domain.builder()
				.id("myDomainInternalId")
				.name("my.domain.internal.com")
				.status(Status.SHARED)
				.type("internal")
				.build(),
			Domain.builder()
				.id("myDomainDefaultId")
				.name("my.domain.default.com")
				.status(Status.SHARED)
				.build(),
			Domain.builder()
				.id("myDomainComId")
				.name("my.domain.com")
				.status(Status.OWNED)
				.build()
		);
	}

	private ApplicationDetail createApplicationDetail() {
		return ApplicationDetail
			.builder()
			.id(APP_ID)
			.stack("")
			.diskQuota(512)
			.instances(1)
			.memoryLimit(512)
			.name(APP_NAME)
			.requestedState("STARTED")
			.runningInstances(1)
			.build();
	}

	private PackageResource createPackage(String dateTime, String id) {
		return PackageResource
			.builder()
			.data(BitsData.builder().build())
			.state(PackageState.READY)
			.type(PackageType.BITS)
			.createdAt(dateTime)
			.updatedAt(dateTime)
			.id(id)
			.build();
	}

	private void verifyRouteCreatedAndMapped(String domainId, String host, String spaceId, String appId) {
		then(routes).should().create(CreateRouteRequest.builder()
			.domainId(domainId)
			.spaceId(spaceId)
			.host(host)
			.build());

		then(applicationsV2).should().associateRoute(AssociateApplicationRouteRequest.builder()
			.applicationId(appId)
			.routeId(getRouteIdForDomain(domainId))
			.build());
	}

	private String getRouteIdForDomain(String domainId) {
		return domainId + "-route";
	}

	private void mockDomainsAndRoutes() {
		given(domains.list()).willReturn(getDomains());
		given(routes.create(any())).willAnswer(invocation -> {
			CreateRouteRequest createRouteRequest = invocation.getArgument(0);
			return Mono.just(CreateRouteResponse.builder()
				.metadata(Metadata
					.builder()
					.id(getRouteIdForDomain(createRouteRequest.getDomainId()))
					.build()).build());
		});
		given(applicationsV2.associateRoute(any()))
			.willReturn(Mono.empty());
	}

	private static Lifecycle createLifecycle() {
		return Lifecycle.builder().data(BuildpackData.builder().build()).type(LifecycleType.BUILDPACK).build();
	}

}
