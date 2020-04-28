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

package org.springframework.cloud.appbroker.logging.recent;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.dropsonde.events.Envelope;
import reactor.core.publisher.Flux;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.LoggingUtils;

class ApplicationRecentLogsProvider implements RecentLogsProvider {

	private final CloudFoundryClient client;

	private final DopplerClient dopplerClient;

	private final ApplicationIdsProvider applicationIdsProvider;

	public ApplicationRecentLogsProvider(CloudFoundryClient client, DopplerClient dopplerClient,
		ApplicationIdsProvider applicationIdsProvider) {
		this.client = client;
		this.dopplerClient = dopplerClient;
		this.applicationIdsProvider = applicationIdsProvider;
	}

	@Override
	public Flux<Envelope> getLogs(String serviceInstanceId) {
		return this.applicationIdsProvider.getApplicationIds(serviceInstanceId)
			.flatMap(this::recentLogs)
			.map(LoggingUtils::convertDopplerEnvelopeToDropsonde);
	}

	protected Flux<org.cloudfoundry.doppler.Envelope> recentLogs(String applicationId) {
		return client.applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build())
			.map(response -> response.getEntity().getName())
			.flatMapMany(appName ->
				dopplerClient.recentLogs(RecentLogsRequest.builder().applicationId(applicationId).build())
					.map(envelope -> LoggingUtils.injectAppNameIntoLogSourceInstance(appName, envelope))
			);
	}

}
