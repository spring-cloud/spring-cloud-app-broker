/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.acceptance.fixtures.cf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.applications.ApplicationEnvironments;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.applications.DeleteApplicationRequest;
import org.cloudfoundry.operations.applications.GetApplicationEnvironmentsRequest;
import org.cloudfoundry.operations.applications.GetApplicationRequest;
import org.cloudfoundry.operations.applications.PushApplicationRequest;
import org.cloudfoundry.operations.applications.SetEnvironmentVariableApplicationRequest;
import org.cloudfoundry.operations.applications.StartApplicationRequest;
import org.cloudfoundry.operations.serviceadmin.CreateServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.DeleteServiceBrokerRequest;
import org.cloudfoundry.operations.serviceadmin.EnableServiceAccessRequest;
import org.cloudfoundry.operations.services.CreateServiceInstanceRequest;
import org.cloudfoundry.operations.services.DeleteServiceInstanceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CloudFoundryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryService.class);

	@Autowired
	private CloudFoundryOperations cloudFoundryOperations;

	@Autowired
	private CloudFoundryProperties cloudFoundryProperties;

	public void enableServiceBrokerAccess(String serviceName) {
		cloudFoundryOperations
			.serviceAdmin()
			.enableServiceAccess(EnableServiceAccessRequest.builder().serviceName(serviceName).build())
			.block();
	}

	public void createServiceBroker(String brokerName, String backingAppURL) {
		cloudFoundryOperations
			.serviceAdmin()
			.create(CreateServiceBrokerRequest.builder()
				.name(brokerName)
				.username("user")
				.password("password")
				.url(backingAppURL)
				.build())
			.block();
	}

	public String getApplicationRoute(String appName) {
		return "https://" + cloudFoundryOperations
			.applications()
			.get(GetApplicationRequest.builder().name(appName).build())
			.block().getUrls().get(0);
	}

	public void startApplication(String appName) {
		cloudFoundryOperations
			.applications()
			.start(StartApplicationRequest.builder().name(appName).build())
			.block();
	}

	public void pushAppNoStart(String appName, Path appPath) {
		cloudFoundryOperations
			.applications()
			.push(PushApplicationRequest
				.builder()
				.noStart(true)
				.path(appPath)
				.name(appName)
				.build())
			.block();
	}

	public void deleteBackingApp(String appName) {
		try {
			cloudFoundryOperations
				.applications()
				.delete(DeleteApplicationRequest.builder().name(appName).build())
				.block();
		} catch (Exception e) {
			// Ignore
		}
	}

	public void deleteServiceBroker(String brokerName) {
		try {
			cloudFoundryOperations
				.serviceAdmin()
				.delete(DeleteServiceBrokerRequest.builder().name(brokerName).build())
				.block();
		} catch (Exception e) {
			// Ignore
		}
	}

	public void deleteServiceInstance(String serviceInstanceName) {
		try {
			cloudFoundryOperations
				.services()
				.deleteInstance(DeleteServiceInstanceRequest.builder().name(serviceInstanceName).build())
				.block();
		} catch (Exception e) {
			// Ignore
		}
	}

	public void createServiceInstance(String planName, String serviceName, String serviceInstanceName) {
		cloudFoundryOperations
			.services()
			.createInstance(CreateServiceInstanceRequest
				.builder()
				.planName(planName)
				.serviceName(serviceName)
				.serviceInstanceName(serviceInstanceName)
				.build())
			.block();
	}

	private static SetEnvironmentVariableApplicationRequest createEnvRequest(String appName, String key, String value) {
		return SetEnvironmentVariableApplicationRequest
			.builder()
			.name(appName)
			.variableName(key)
			.variableValue(value)
			.build();
	}

	public List<ApplicationSummary> getApplications() {
		return cloudFoundryOperations.applications().list().collectList().block();
	}

	public ApplicationEnvironments getApplicationEnvironmentByAppName(String appName) {
		return cloudFoundryOperations
			.applications()
			.getEnvironments(GetApplicationEnvironmentsRequest.builder().name(appName).build())
			.block();
	}

	public void setBrokerAppEnvironment(String[] properties) {
		Flux<Void> catalogPublishers = getCatalogPublishers();
		Flux<Void> appBrokerCFPublishers = getAppBrokerCFPublishers();
		Flux<Void> appBrokerApplicationPublishers = Flux.concat(Arrays.stream(properties)
			.filter(property -> property.contains("="))
			.map(property -> property.split("="))
			.map(property -> setEnvRequest(property[0], property[1]))
			.collect(Collectors.toList()));

		Flux.concat(catalogPublishers, appBrokerCFPublishers, appBrokerApplicationPublishers).blockLast();
	}

	private Flux<Void> getAppBrokerCFPublishers() {
		return Flux.concat(
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.api-host", cloudFoundryProperties.getApiHost()),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.api-port", String.valueOf(cloudFoundryProperties.getApiPort())),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.username", cloudFoundryProperties.getUsername()),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.password", cloudFoundryProperties.getPassword()),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.default-org", cloudFoundryProperties.getDefaultOrg()),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.default-space", cloudFoundryProperties.getDefaultSpace()),
			setEnvRequest("spring.cloud.appbroker.deployer.cloudfoundry.skip-ssl-validation", String.valueOf(cloudFoundryProperties.isSkipSslValidation()))
		);
	}

	private Flux<Void> getCatalogPublishers() {
		return Flux.concat(
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].id", "example-service"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].name", "example"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].description", "A simple example"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].bindable", "true"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].tags[0]", "example"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].plans[0].id", "simple-plan"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].plans[0].bindable", "true"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].plans[0].name", "standard"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].plans[0].description", "A simple plan"),
			setEnvRequest("spring.cloud.openservicebroker.catalog.services[0].plans[0].free", "true")
		);
	}

	private Mono<Void> setEnvRequest(String key, String value) {
		return cloudFoundryOperations
			.applications()
			.setEnvironmentVariable(createEnvRequest("sample-broker", key, value))
			.doOnSuccess(v -> LOGGER.info("Environment with key {} set", key));
	}

}
