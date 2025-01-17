/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.appbroker.deployer;

import java.util.Objects;

import org.springframework.util.CollectionUtils;

public class BrokeredService {

	private String serviceName;

	private String planName;

	private BackingApplications apps;

	private BackingServices services;

	private TargetSpec target;

	private BrokeredService() {
		super();
	}

	public BrokeredService(String serviceName, String planName, BackingApplications apps, BackingServices services,
			TargetSpec target) {
		super();
		this.serviceName = serviceName;
		this.planName = planName;
		this.apps = apps;
		this.services = services;
		this.target = target;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getPlanName() {
		return this.planName;
	}

	public void setPlanName(String planName) {
		this.planName = planName;
	}

	public BackingApplications getApps() {
		return this.apps;
	}

	public void setApps(BackingApplications apps) {
		this.apps = apps;
	}

	public BackingServices getServices() {
		return this.services;
	}

	public void setServices(BackingServices services) {
		this.services = services;
	}

	public TargetSpec getTarget() {
		return this.target;
	}

	public void setTarget(TargetSpec target) {
		this.target = target;
	}

	public static BrokeredServiceBuilder builder() {
		return new BrokeredServiceBuilder();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BrokeredService)) {
			return false;
		}
		BrokeredService that = (BrokeredService) o;
		return Objects.equals(this.serviceName, that.serviceName) && Objects.equals(this.planName, that.planName)
				&& Objects.equals(this.apps, that.apps) && Objects.equals(this.services, that.services)
				&& Objects.equals(this.target, that.target);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(this.serviceName, this.planName, this.apps, this.services, this.target);
	}

	@Override
	public String toString() {
		return "BrokeredService{" + "serviceName='" + this.serviceName + '\'' + ", planName='" + this.planName + '\''
				+ ", apps=" + this.apps + ", services=" + this.services + ", target=" + this.target + '}';
	}

	public static class BrokeredServiceBuilder {

		private String id;

		private String planId;

		private BackingApplications backingApplications;

		private BackingServices backingServices;

		private TargetSpec target;

		public BrokeredServiceBuilder serviceName(String id) {
			this.id = id;
			return this;
		}

		public BrokeredServiceBuilder planName(String planId) {
			this.planId = planId;
			return this;
		}

		public BrokeredServiceBuilder apps(BackingApplications backingApplications) {
			if (!CollectionUtils.isEmpty(backingApplications)) {
				this.backingApplications = BackingApplications.builder()
					.backingApplications(backingApplications)
					.build();
			}
			return this;
		}

		public BrokeredServiceBuilder services(BackingServices backingServices) {
			if (!CollectionUtils.isEmpty(backingServices)) {
				this.backingServices = BackingServices.builder().backingServices(backingServices).build();
			}
			return this;
		}

		public BrokeredServiceBuilder target(TargetSpec target) {
			this.target = target;
			return this;
		}

		public BrokeredService build() {
			return new BrokeredService(this.id, this.planId, this.backingApplications, this.backingServices,
					this.target);
		}

	}

}
