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

package org.springframework.cloud.appbroker.workflow.instance;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cloud.appbroker.deployer.BackingService;

/**
 * Validates updates to backing services. Throws a exception if invalid.
 * When valid, returns an updated copy of the list with the previousPlan set.
 * This helps only triggering plan update when previousPlan is set.
 */
public class BackingServicesUpdateValidatorService {

	public List<BackingService> validateAndMutatePlanUpdate(List<BackingService> previousBackingServicesList,
		List<BackingService> candidateBackingServicesList) {
		return candidateBackingServicesList.stream()
			.map(candidateBackingService -> {
				BackingService matchingPreviousBackingService = getValidatedBackingService(candidateBackingService,
					previousBackingServicesList);
				if (candidateBackingService.getPlan().equals(matchingPreviousBackingService.getPlan())) {
					return candidateBackingService;
				}
				else {
					//Don't mutate the caller's BackingService bean (which has not a "request" scope),
					//return a copy instead.
					return BackingService.builder()
						.backingService(candidateBackingService)
						.previousPlan(matchingPreviousBackingService.getPlan())
						.build();
				}

			})
			.collect(Collectors.toList());
	}

	private BackingService getValidatedBackingService(BackingService candidate,
		List<BackingService> previousBackingServicesList) {
		List<BackingService> matchingPreviousBackingServices = previousBackingServicesList.stream()
			.filter(previousBackingService ->
				previousBackingService.getName().equals(candidate.getName()) &&
					previousBackingService.getServiceInstanceName().equals(candidate.getServiceInstanceName()))
			.collect(Collectors.toList());
		if (matchingPreviousBackingServices.size() != 1) { // NOPMD
			throw new IllegalArgumentException(
				"Unsupported update that would result into backing services changes other than plan upgrade. " +
					"Candidate after update= " + candidate + " Before update=" + previousBackingServicesList);
		}
		return matchingPreviousBackingServices.get(0);
	}

}
