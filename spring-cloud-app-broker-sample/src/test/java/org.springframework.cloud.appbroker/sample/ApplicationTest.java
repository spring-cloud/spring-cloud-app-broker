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

package org.springframework.cloud.appbroker.sample;

import org.cloudfoundry.operations.CloudFoundryOperations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentService;
import org.springframework.cloud.appbroker.deployer.ReactiveAppDeployer;
import org.springframework.cloud.appbroker.service.WorkflowServiceInstanceService;
import org.springframework.cloud.servicebroker.controller.CatalogController;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {AppBrokerSampleApplication.class})
@TestPropertySource({
	"classpath:application-openservicebroker-catalog.yml",
	"classpath:application-appbroker-cf.yml"
})
class ApplicationTest {
	@Autowired(required = false)
	private CatalogController catalogController;

	@Autowired
	private CloudFoundryOperations cloudFoundryOperations;

	@Autowired(required = false)
	private ReactiveAppDeployer appDeployer;

	@Autowired(required = false)
	private BackingAppDeploymentService deploymentService;

	@Autowired(required = false)
	private WorkflowServiceInstanceService serviceInstanceService;

	@Test
	void applicationInitialized() {
		assertThat(catalogController).isNotNull();
		assertThat(cloudFoundryOperations).isNotNull();
		assertThat(appDeployer).isNotNull();
		assertThat(deploymentService).isNotNull();
		assertThat(serviceInstanceService).isNotNull();
	}
}
