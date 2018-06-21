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

package org.springframework.cloud.appbroker.serviceinstance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.cloud.appbroker.deployer.BackingAppDeployProperties;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.deployer.DeployerApplication;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProvisionServiceInstanceWorkflowTest {

	private BackingAppDeployProperties backingAppDeployProperties;
	@Mock
	private BackingAppDeploymentPlan backingAppDeploymentPlan;
	private ProvisionServiceInstanceWorkflow provisionServiceInstanceWorkflow;

	@Test
	void shouldProvisionDefaultServiceInstance() {
		// given that properties contains app name
		backingAppDeployProperties = new BackingAppDeployProperties();
		backingAppDeployProperties.setAppName("helloworldapp");
		backingAppDeployProperties.setPath("http://myfiles/app.jar");
		provisionServiceInstanceWorkflow = new ProvisionServiceInstanceWorkflow(backingAppDeployProperties, backingAppDeploymentPlan);

		// when
		provisionServiceInstanceWorkflow.provision();

		//then deployer should be called with the application name
		DeployerApplication expectedDeployerApplication = new DeployerApplication();
		expectedDeployerApplication.setPath("http://myfiles/app.jar");
		expectedDeployerApplication.setAppName("helloworldapp");

		verify(backingAppDeploymentPlan, times(1))
			.execute(expectedDeployerApplication);
	}
}