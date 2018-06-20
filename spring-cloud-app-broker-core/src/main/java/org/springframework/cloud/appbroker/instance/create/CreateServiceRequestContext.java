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

package org.springframework.cloud.appbroker.instance.create;

import java.util.Set;

import org.springframework.cloud.appbroker.context.BrokerRequestContext;
import org.springframework.cloud.appbroker.instance.app.BackingAppState;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;

/**
 * Holder for data that is specific within the context of a single service broker request
 */
public class CreateServiceRequestContext implements BrokerRequestContext {

	private Set<BackingAppState> backingAppState;
	private CreateServiceInstanceRequest originatingRequest;

	public CreateServiceRequestContext(CreateServiceInstanceRequest requestData) {
		this.originatingRequest = requestData;
	}

	public Set<BackingAppState> getBackingAppState() {
		return backingAppState;
	}

	public void addBackingAppState(BackingAppState backingAppState) {
		this.backingAppState.add( backingAppState);
	}

	public CreateServiceInstanceRequest getOriginatingRequest() {
		return originatingRequest;
	}

	public void setOriginatingRequest(CreateServiceInstanceRequest originatingRequest) {
		this.originatingRequest = originatingRequest;
	}
}
