/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.cloud.appbroker.logging.streaming.events;

import org.springframework.context.ApplicationEvent;

public class ServiceInstanceLoggingEvent extends ApplicationEvent {

	private static final long serialVersionUID = 3721553379568462887L;

	public enum Operation {

		/**
		 * Start publishing log stream for a given service instance id
		 */
		START,

		/**
		 * Stop publishing log stream for a given service instance id
		 */
		STOP
	}

	private final String serviceInstanceId;

	private final Operation operation;

	public ServiceInstanceLoggingEvent(Object source, String serviceInstanceId, Operation operation) {
		super(source);
		this.serviceInstanceId = serviceInstanceId;
		this.operation = operation;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public Operation getOperation() {
		return operation;
	}

}
