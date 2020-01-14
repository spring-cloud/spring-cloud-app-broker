/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

public class BackingServices extends ArrayList<BackingService> {

	private static final long serialVersionUID = 1L;

	private BackingServices() {
		super();
	}

	public BackingServices(List<BackingService> backingServices) {
		super(backingServices);
	}

	public static BackingServicesBuilder builder() {
		return new BackingServicesBuilder();
	}

	public static class BackingServicesBuilder {

		private final List<BackingService> backingServices = new ArrayList<>();

		public BackingServicesBuilder backingService(BackingService backingService) {
			if (backingService != null) {
				this.backingServices.add(backingService);
			}
			return this;
		}

		public BackingServicesBuilder backingServices(BackingServices backingServices) {
			if (!CollectionUtils.isEmpty(backingServices)) {
				backingServices.forEach(backingService -> this.backingService(BackingService.builder()
					.backingService(backingService)
					.build()));
			}
			return this;
		}

		public BackingServices build() {
			return new BackingServices(backingServices);
		}

	}

}
