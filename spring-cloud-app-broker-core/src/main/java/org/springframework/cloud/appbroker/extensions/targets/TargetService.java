/*
 * Copyright 2016-2018. the original author or authors.
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

package org.springframework.cloud.appbroker.extensions.targets;

import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.deployer.BackingApplication;
import org.springframework.cloud.appbroker.deployer.TargetSpec;
import org.springframework.cloud.appbroker.extensions.ExtensionLocator;

public class TargetService {

	private final ExtensionLocator<Target> locator;

	public TargetService(List<TargetFactory<?>> factories) {
		locator = new ExtensionLocator<>(factories);
	}

	public Mono<List<BackingApplication>> add(List<BackingApplication> backingApplications,
											  String serviceInstanceId) {

		return Flux.fromIterable(backingApplications)
				   .flatMap(backingApplication -> {
					   TargetSpec targetSpec = backingApplication.getTarget();
					   if (targetSpec != null) {
						   Target target = locator.getByName(targetSpec.getName(), Collections.emptyMap());
						   return target.apply(backingApplication, serviceInstanceId);
					   }
					   return Mono.just(backingApplication);

				   })
				   .collectList();
	}

}
