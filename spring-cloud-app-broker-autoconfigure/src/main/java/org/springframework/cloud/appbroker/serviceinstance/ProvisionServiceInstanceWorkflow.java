/*
 * Copyright 2016-2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package org.springframework.cloud.appbroker.serviceinstance;

import org.springframework.cloud.appbroker.deployer.BackingAppDeployProperties;
import org.springframework.cloud.appbroker.deployer.BackingAppDeploymentPlan;
import org.springframework.cloud.appbroker.deployer.DeployerApplication;
import org.springframework.stereotype.Service;

//TODO This should be in the App Broker core subproject
@Service
class ProvisionServiceInstanceWorkflow {

	private BackingAppDeployProperties backingAppDeployProperties;

	private BackingAppDeploymentPlan deploymentPlan;

	public ProvisionServiceInstanceWorkflow(BackingAppDeployProperties backingAppDeployProperties,
											BackingAppDeploymentPlan deploymentPlan) {
		this.backingAppDeployProperties = backingAppDeployProperties;
		this.deploymentPlan = deploymentPlan;
	}

	void provision() {
		final String appName = backingAppDeployProperties.getAppName();
		deploymentPlan.execute(createDeployerApplication(appName));
	}

	private DeployerApplication createDeployerApplication(String appName) {
		return DeployerApplication.DeployerApplicationBuilder.builder().withAppName(appName).build();
	}
}
