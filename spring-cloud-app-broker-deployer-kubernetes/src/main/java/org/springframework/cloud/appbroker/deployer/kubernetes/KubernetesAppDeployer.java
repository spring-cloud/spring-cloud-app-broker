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

import java.util.Collections;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodTemplateSpecBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpecBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.AppDeployer;
import org.springframework.cloud.appbroker.deployer.DeployApplicationRequest;
import org.springframework.cloud.appbroker.deployer.DeployApplicationResponse;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import static java.util.Collections.singletonMap;

public class KubernetesAppDeployer implements AppDeployer, ResourceLoaderAware {

	private final Logger logger = LoggerFactory.getLogger(KubernetesAppDeployer.class);

	private final KubernetesClient kubernetesClient;
	private final KubernetesTargetProperties targetProperties;

	private ResourceLoader resourceLoader;

	public KubernetesAppDeployer(KubernetesClient kubernetesClient,
								 KubernetesTargetProperties targetProperties,
								 ResourceLoader resourceLoader) {
		this.kubernetesClient = kubernetesClient;
		this.targetProperties = targetProperties;
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Mono<DeployApplicationResponse> deploy(DeployApplicationRequest request) {
		logger.debug("Deploying application: name={}, path={}",
				request.getName(), request.getPath());

		Container container = new Container();
		container.setImage(request.getPath());
		container.setName(request.getName());

		ContainerPort containerPort = new ContainerPort();
		containerPort.setContainerPort(8080);
		container.setPorts(Collections.singletonList(containerPort));

		Deployment deployment = new Deployment();

		ObjectMeta objectMeta = new ObjectMeta();
		objectMeta.setName(request.getName() + "-deployment");

		deployment.setMetadata(objectMeta);

		DeploymentSpec deploymentSpec =
				new DeploymentSpecBuilder()
						.withSelector(
								new LabelSelectorBuilder()
										.withMatchLabels(singletonMap("app", "app-broker-app"))
										.build())
						.build();

		deploymentSpec.setTemplate(
				new PodTemplateSpecBuilder()
						.withNewMetadata()
						.withLabels(singletonMap("app", "app-broker-app"))
						.endMetadata()
						.withNewSpec()
						.withContainers(container)
						.endSpec()
						.build()
		);

		deployment.setSpec(deploymentSpec);

		kubernetesClient
				.apps()
				.deployments()
				.inNamespace("default")
				.create(deployment);

		logger.debug("Deployed application: name={}, path={}",
				request.getName(), request.getPath());

		return Mono.just(DeployApplicationResponse
				.builder()
				.name(request.getName())
				.build());
	}

}
