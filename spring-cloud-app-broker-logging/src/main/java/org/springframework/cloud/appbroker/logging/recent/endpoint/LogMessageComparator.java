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

package org.springframework.cloud.appbroker.logging.recent.endpoint;

import java.util.Comparator;

import org.cloudfoundry.logcache.v1.Envelope;

class LogMessageComparator implements Comparator<Envelope> {

	@Override
	public int compare(Envelope o1, Envelope o2) {
		return Long.compare(getTimestamp(o1), getTimestamp(o2));
	}

	private long getTimestamp(Envelope e) {
		if (e.getTimestamp() != null) {
			return e.getTimestamp();
		}
		else {
			return 0;
		}
	}

}
