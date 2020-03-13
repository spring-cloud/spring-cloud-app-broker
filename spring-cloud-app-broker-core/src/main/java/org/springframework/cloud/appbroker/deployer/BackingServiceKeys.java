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

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class BackingServiceKeys extends ArrayList<BackingServiceKey> {

	private static final long serialVersionUID = 1L;

	private BackingServiceKeys() {
	}

	public BackingServiceKeys(List<BackingServiceKey> backingServiceKeys) {
		super.addAll(backingServiceKeys);
	}

	public static BackingServiceKeysBuilder builder() {
		return new BackingServiceKeysBuilder();
	}

	public static class BackingServiceKeysBuilder {

		private final List<BackingServiceKey> backingServiceKeys = new ArrayList<>();

		public BackingServiceKeysBuilder backingServiceKey(BackingServiceKey backingServiceKey) {
			if (backingServiceKey != null) {
				this.backingServiceKeys.add(backingServiceKey);
			}
			return this;
		}

		public BackingServiceKeysBuilder backingServiceKeys(BackingServiceKeys backingServiceKeys) {
			if (!CollectionUtils.isEmpty(backingServiceKeys)) {
				backingServiceKeys.forEach(backingServiceKey -> this.backingServiceKey(BackingServiceKey.builder()
					.backingService(backingServiceKey)
					.build()));
			}
			return this;
		}

		public BackingServiceKeys build() {
			return new BackingServiceKeys(backingServiceKeys);
		}
	}
}
