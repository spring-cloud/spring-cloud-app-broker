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

package org.springframework.cloud.appbroker.deployer.cloudfoundry;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ApplicationsV2;
import org.cloudfoundry.client.v2.applications.UpdateApplicationResponse;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Relationship;
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
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.Packages;
import org.cloudfoundry.client.v3.packages.UploadPackageResponse;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationDetail;
import org.cloudfoundry.operations.applications.Applications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.UpdateApplicationRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudFoundryAppDeployerUpdateApplicationTest {

	private static final String APP_NAME = "test-app";
	private static final String APP_PATH = "test.jar";

	private AppDeployer appDeployer;

	@Mock
	private Applications operationsApplications;

	@Mock
	private ApplicationsV2 applicationsV2;

	@Mock
	private Builds builds;

	@Mock
	private DeploymentsV3 deploymentsV3;

	@Mock
	private Packages packages;

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

		when(operationsApplications.pushManifest(any())).thenReturn(Mono.empty());
		when(resourceLoader.getResource(APP_PATH)).thenReturn(new FileSystemResource(APP_PATH));

		when(cloudFoundryOperations.applications()).thenReturn(operationsApplications);
		when(cloudFoundryClient.applicationsV2()).thenReturn(applicationsV2);
		when(cloudFoundryClient.packages()).thenReturn(packages);
		when(cloudFoundryClient.builds()).thenReturn(builds);
		when(cloudFoundryClient.deploymentsV3()).thenReturn(deploymentsV3);
		when(operationsUtils.getOperations(anyMap())).thenReturn(Mono.just(cloudFoundryOperations));
		when(operationsUtils.getOperationsForSpace(anyString())).thenReturn(Mono.just(cloudFoundryOperations));

		appDeployer = new CloudFoundryAppDeployer(deploymentProperties,
			cloudFoundryOperations, cloudFoundryClient, operationsUtils, targetProperties, resourceLoader);
	}

	@Test
	void updateApp() {
		when(operationsApplications.get(any()))
			.thenReturn(Mono.just(createApplicationDetail()));
		when(applicationsV2.update(any()))
			.thenReturn(Mono.just(UpdateApplicationResponse.builder().build()));
		when(packages.create(any()))
			.thenReturn(Mono.just(CreatePackageResponse
				.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("DATETIME")
				.id("package-id")
				.build()));

		when(packages.upload(any()))
			.thenReturn(Mono.just(UploadPackageResponse
				.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("DATETIME")
				.id("package-id")
				.build()));


		when(packages.get(any()))
			.thenReturn(Mono.just(GetPackageResponse
				.builder()
				.data(BitsData.builder().build())
				.state(PackageState.READY)
				.type(PackageType.BITS)
				.createdAt("DATETIME")
				.id("package-id")
				.build()));

		when(builds.create(any()))
			.thenReturn(Mono.just(CreateBuildResponse
				.builder()
				.state(BuildState.STAGING)
				.createdBy(CreatedBy.builder().id("create-by-id").email("an-email").name("creator").build())
				.inputPackage(Relationship.builder().id("package-id").build())
				.lifecycle(createLifecycle())
				.createdAt("DATETIME")
				.id("build-id")
				.build()));
		when(builds.get(any()))
			.thenReturn(Mono.just(GetBuildResponse
				.builder()
				.state(BuildState.STAGED)
				.createdBy(CreatedBy.builder().id("create-by-id").email("an-email").name("creator").build())
				.inputPackage(Relationship.builder().id("package-id").build())
				.lifecycle(createLifecycle())
				.droplet(Droplet.builder().id("droplet-id").build())
				.createdAt("DATETIME")
				.id("build-id")
				.build()));

		when(deploymentsV3.create(any()))
			.thenReturn(Mono.just(CreateDeploymentResponse
				.builder()
				.state(DeploymentState.DEPLOYED)
				.createdAt("DATETIME")
				.id("deployment-id")
				.build()));
		when(deploymentsV3.get(any()))
			.thenReturn(Mono.just(GetDeploymentResponse
				.builder()
				.state(DeploymentState.DEPLOYED)
				.createdAt("DATETIME")
				.id("deployment-id")
				.build()));

		UpdateApplicationRequest request =
			UpdateApplicationRequest
				.builder()
				.name(APP_NAME)
				.path(APP_PATH)
				.build();

		StepVerifier.create(appDeployer.update(request))
					.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
					.verifyComplete();
	}

	private ApplicationDetail createApplicationDetail() {
		return ApplicationDetail
			.builder()
			.id("app-id")
			.stack("")
			.diskQuota(512)
			.instances(1)
			.memoryLimit(512)
			.name("app")
			.requestedState("STARTED")
			.runningInstances(1)
			.build();
	}

	private static Lifecycle createLifecycle() {
		return Lifecycle.builder().data(BuildpackData.builder().build()).type(LifecycleType.BUILDPACK).build();
	}

}