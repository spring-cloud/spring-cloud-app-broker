package org.springframework.cloud.appbroker.workflow.action.createserviceinstance.appdeploy;

public class BackingAppDeploymentPlan {
	private BackingAppDeployer backingAppDeployer;

	private BackingAppParameters backingAppParameters;


	public BackingAppDeploymentPlan(BackingAppDeployer backingAppDeployer, BackingAppParameters backingAppParameters) {
		this.backingAppDeployer = backingAppDeployer;
		this.backingAppParameters = backingAppParameters;
	}

	public BackingAppDeployer getBackingAppDeployer() {
		return backingAppDeployer;
	}

	public BackingAppParameters getBackingAppParameters() {
		return backingAppParameters;
	}

}
