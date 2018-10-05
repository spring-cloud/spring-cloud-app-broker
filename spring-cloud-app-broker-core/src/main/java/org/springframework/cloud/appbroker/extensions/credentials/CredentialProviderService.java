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

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.CredentialProviderSpec;
import org.springframework.cloud.appbroker.extensions.ExtensionLocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class CredentialProviderService {

	private final ExtensionLocator<CredentialProvider> locator;

	public CredentialProviderService(List<CredentialProviderFactory<?>> factories) {
		locator = new ExtensionLocator<>(factories);
	}

	public Mono<List<BackingApplication>> addCredentials(List<BackingApplication> backingApplications,
														 String serviceInstanceGuid) {
		return Flux.fromIterable(backingApplications)
			.flatMap(backingApplication -> {
				List<CredentialProviderSpec> specs = getSpecsForApplication(backingApplication);

				return Flux.fromIterable(specs)
					.flatMap(spec -> {
						CredentialProvider provider = locator.getByName(spec.getName(), spec.getArgs());
						return provider.addCredentials(backingApplication, serviceInstanceGuid);
					})
					.then(Mono.just(backingApplication));
			})
			.collectList();
	}

	public Mono<List<BackingApplication>> deleteCredentials(List<BackingApplication> backingApplications,
															String serviceInstanceGuid) {
		return Flux.fromIterable(backingApplications)
			.flatMap(backingApplication -> {
				List<CredentialProviderSpec> specs = getSpecsForApplication(backingApplication);

				return Flux.fromIterable(specs)
					.flatMap(spec -> {
						CredentialProvider provider = locator.getByName(spec.getName(), spec.getArgs());
						return provider.deleteCredentials(backingApplication, serviceInstanceGuid);
					})
					.then(Mono.just(backingApplication));
			})
			.collectList();
	}

	private List<CredentialProviderSpec> getSpecsForApplication(BackingApplication backingApplication) {
		return backingApplication.getParametersTransformers() == null
			? Collections.emptyList()
			: backingApplication.getCredentialProviders();
	}
}
