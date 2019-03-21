/*
 * Copyright 2016-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.workflow.instance;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.cloud.appbroker.deployer.BackingServicesProvisionService;
import org.springframework.cloud.appbroker.deployer.BrokeredServices;
import org.springframework.cloud.appbroker.extensions.credentials.CredentialProviderService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingApplicationsParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.parameters.BackingServicesParametersTransformationService;
import org.springframework.cloud.appbroker.extensions.targets.TargetService;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse.CreateServiceInstanceResponseBuilder;
import org.springframework.core.annotation.Order;

@Order(0)
public class AppDeploymentCreateServiceInstanceWorkflow
	extends AppDeploymentInstanceWorkflow
	implements CreateServiceInstanceWorkflow {

	private final Logger log = Loggers.getLogger(AppDeploymentCreateServiceInstanceWorkflow.class);

	private final BackingAppDeploymentService deploymentService;
	private final BackingServicesProvisionService backingServicesProvisionService;
	private final BackingApplicationsParametersTransformationService appsParametersTransformationService;
	private final BackingServicesParametersTransformationService servicesParametersTransformationService;
	private final CredentialProviderService credentialProviderService;
	private final TargetService targetService;

	public AppDeploymentCreateServiceInstanceWorkflow(BrokeredServices brokeredServices,
													  BackingAppDeploymentService deploymentService,
													  BackingServicesProvisionService backingServicesProvisionService,
													  BackingApplicationsParametersTransformationService appsParametersTransformationService,
													  BackingServicesParametersTransformationService servicesParametersTransformationService,
													  CredentialProviderService credentialProviderService,
													  TargetService targetService) {
		super(brokeredServices);
		this.deploymentService = deploymentService;
		this.backingServicesProvisionService = backingServicesProvisionService;
		this.appsParametersTransformationService = appsParametersTransformationService;
		this.servicesParametersTransformationService = servicesParametersTransformationService;
		this.credentialProviderService = credentialProviderService;
		this.targetService = targetService;
	}

	@Override
	public Mono<Void> create(CreateServiceInstanceRequest request, CreateServiceInstanceResponse response) {
		return createBackingServices(request)
			.thenMany(deployBackingApplications(request))
			.then();
	}

	private Flux<String> createBackingServices(CreateServiceInstanceRequest request) {
		return getBackingServicesForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingServices ->
				targetService.addToBackingServices(backingServices,
					getTargetForService(request.getServiceDefinition(), request.getPlan()) ,
					request.getServiceInstanceId()))
			.flatMap(backingServices ->
				servicesParametersTransformationService.transformParameters(backingServices,
					request.getParameters()))
			.flatMapMany(backingServicesProvisionService::createServiceInstance)
			.doOnRequest(l -> log.debug("Creating backing services for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished creating backing services for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error("Error creating backing services for {}/{} with error '{}'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exception.getMessage()));
	}

	private Flux<String> deployBackingApplications(CreateServiceInstanceRequest request) {
		return getBackingApplicationsForService(request.getServiceDefinition(), request.getPlan())
			.flatMap(backingApps ->
				targetService.addToBackingApplications(backingApps,
					getTargetForService(request.getServiceDefinition(),
						request.getPlan()) , request.getServiceInstanceId()))
			.flatMap(backingApps ->
				appsParametersTransformationService.transformParameters(backingApps,
					request.getParameters()))
			.flatMap(backingApps ->
				credentialProviderService.addCredentials(backingApps,
					request.getServiceInstanceId()))
			.flatMapMany(backingApps -> deploymentService.deploy(backingApps, request.getServiceInstanceId()))
			.doOnRequest(l -> log.debug("Deploying backing applications for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnComplete(() -> log.debug("Finished deploying backing applications for {}/{}",
				request.getServiceDefinition().getName(), request.getPlan().getName()))
			.doOnError(exception -> log.error("Error deploying backing applications for {}/{} with error '{}'",
				request.getServiceDefinition().getName(), request.getPlan().getName(), exception.getMessage()));
	}

	@Override
	public Mono<Boolean> accept(CreateServiceInstanceRequest request) {
		return accept(request.getServiceDefinition(), request.getPlan());
	}

	@Override
	public Mono<CreateServiceInstanceResponseBuilder> buildResponse(CreateServiceInstanceRequest request,
																	CreateServiceInstanceResponseBuilder responseBuilder) {
		return Mono.just(responseBuilder.async(true));
	}
}
