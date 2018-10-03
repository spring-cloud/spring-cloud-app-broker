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

package org.springframework.cloud.appbroker.extensions.credentials;

import org.springframework.cloud.appbroker.extensions.AbstractExtensionFactory;

public abstract class CredentialProviderFactory<C> extends AbstractExtensionFactory<CredentialProvider, C> {
	protected CredentialProviderFactory() {
		super();
	}

	protected CredentialProviderFactory(Class<C> configClass) {
		super(configClass);
	}

	@Override
	public abstract CredentialProvider create(C config);

	public String getName() {
		return getShortName(CredentialProviderFactory.class);
	}
}
