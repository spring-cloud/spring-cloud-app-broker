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

package org.springframework.cloud.appbroker.deployer.kubernetes;

import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.test.StepVerifier;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KubernetesAppDeployerTest {

	private static final String APP_NAME = "test-app";
	private static final String APP_PATH = "test.jar";
	private static final String SERVICE_INSTANCE_ID = "service-instance-id";

	private AppDeployer appDeployer;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private KubernetesClient kubernetesClient;

	@Mock
	private ResourceLoader resourceLoader;

	@Mock
	private MixedOperation<Deployment, DeploymentList, DoneableDeployment, RollableScalableResource<Deployment, DoneableDeployment>> deployments;

	@Mock
	private NonNamespaceOperation<Deployment, DeploymentList, DoneableDeployment, RollableScalableResource<Deployment, DoneableDeployment>> deploymentsOperation;

	@BeforeEach
	void setUp() {
		KubernetesTargetProperties targetProperties = new KubernetesTargetProperties();
		targetProperties.setMasterUrl("https://master-url.com");

		when(kubernetesClient.apps().deployments()).thenReturn(deployments);

		when(deployments.inNamespace(anyString())).thenReturn(deploymentsOperation);

		when(resourceLoader.getResource(APP_PATH))
				.thenReturn(new FileSystemResource(APP_PATH));

		appDeployer =
				new KubernetesAppDeployer(kubernetesClient,
						targetProperties,
						resourceLoader);
	}

	@Test
	void deployAppWithPlatformDefaults() {
		DeployApplicationRequest request =
				DeployApplicationRequest.builder()
										.name(APP_NAME)
										.path(APP_PATH)
										.serviceInstanceId(SERVICE_INSTANCE_ID)
										.build();

		StepVerifier.create(appDeployer.deploy(request))
					.assertNext(response -> assertThat(response.getName()).isEqualTo(APP_NAME))
					.verifyComplete();

		PodSpec podSpec = new PodSpec();
//		podSpec.setContainers(); // TODO

		PodTemplateSpec podTemplateSpec = new PodTemplateSpec();
		podTemplateSpec.setSpec(podSpec);

		DeploymentSpec deploymentSpec = new DeploymentSpec();
		deploymentSpec.setTemplate(podTemplateSpec);
		Deployment expectedDeployment = new Deployment();
		expectedDeployment.setSpec(deploymentSpec);

		verify(deploymentsOperation).create(expectedDeployment);
	}

}
