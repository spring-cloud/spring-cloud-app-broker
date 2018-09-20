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

package org.springframework.cloud.appbroker.deployer;

import java.util.ArrayList;
import java.util.List;

public class BackingApplications extends ArrayList<BackingApplication> {
	private static final long serialVersionUID = 159473836238657105L;

	public BackingApplications() {
	}

	public BackingApplications(BackingApplications backingApplicationsToCopy) {
		backingApplicationsToCopy.forEach(backingApplicationToCopy ->
			this.add(new BackingApplication(backingApplicationToCopy)));
	}

	private BackingApplications(List<BackingApplication> backingApplications) {
		super.addAll(backingApplications);
	}

	public static BackingApplicationsBuilder builder() {
		return new BackingApplicationsBuilder();
	}

	public static class BackingApplicationsBuilder {
		private List<BackingApplication> backingApplications = new ArrayList<>();

		public BackingApplicationsBuilder backingApplication(BackingApplication backingApplication) {
			this.backingApplications.add(backingApplication);
			return this;
		}

		public BackingApplications build() {
			return new BackingApplications(backingApplications);
		}
	}
}
