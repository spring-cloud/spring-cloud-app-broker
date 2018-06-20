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

package org.springframework.cloud.appbroker.instance.app;

public class BackingAppDeploymentPlan {
	private String deploymentPlanId;
	private BackingAppDeployer backingAppDeployer;
	private BackingAppParameters backingAppParameters;


	public BackingAppDeploymentPlan(String deploymentPlanId, BackingAppDeployer backingAppDeployer, BackingAppParameters backingAppParameters) {
		this.deploymentPlanId = deploymentPlanId;
		this.backingAppDeployer = backingAppDeployer;
		this.backingAppParameters = backingAppParameters;
	}

	public BackingAppDeployer getBackingAppDeployer() {
		return backingAppDeployer;
	}

	public String getDeploymentPlanId() {
		return deploymentPlanId;
	}

	public BackingAppParameters getBackingAppParameters() {
		return backingAppParameters;
	}

}
