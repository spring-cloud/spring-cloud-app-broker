/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cloud.appbroker.extensions.targets;

import org.springframework.cloud.appbroker.extensions.AbstractExtensionFactory;

public abstract class TargetFactory<C> extends AbstractExtensionFactory<Target, C> {

	protected TargetFactory() {
		super();
	}

	public TargetFactory(Class<C> configClass) {
		super(configClass);
	}

	@Override
	public abstract Target create(C config);

	@Override
	public String getName() {
		return getShortName(TargetFactory.class);
	}

}
