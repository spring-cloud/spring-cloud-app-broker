/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

public class GetApplicationStatusResponse {

	private String deploymentId;

	private String status;

	GetApplicationStatusResponse(String deploymentId, String status) {
		this.deploymentId = deploymentId;
		this.status = status;
	}

	public static GetApplicationStatusResponseBuilder builder() {
		return new GetApplicationStatusResponseBuilder();
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public String getStatus() {
		return status;
	}

	public static class GetApplicationStatusResponseBuilder {

		private String deploymentId;

		private String status;

		GetApplicationStatusResponseBuilder() {

		}

		public GetApplicationStatusResponseBuilder deploymentId(String deploymentId) {
			this.deploymentId = deploymentId;
			return this;
		}

		public GetApplicationStatusResponseBuilder status(String status) {
			this.status = status;
			return this;
		}

		public GetApplicationStatusResponse build() {
			return new GetApplicationStatusResponse(deploymentId, status);
		}

	}

}
