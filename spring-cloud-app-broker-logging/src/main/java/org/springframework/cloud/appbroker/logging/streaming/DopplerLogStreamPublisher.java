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

package org.springframework.cloud.appbroker.logging.streaming;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.StreamRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.cloud.appbroker.logging.ApplicationIdsProvider;
import org.springframework.cloud.appbroker.logging.LoggingUtils;

public class DopplerLogStreamPublisher implements LogStreamPublisher<Envelope> {

	private static final Logger LOG = LoggerFactory.getLogger(DopplerLogStreamPublisher.class);

	private final CloudFoundryClient client;

	private final DopplerClient dopplerClient;

	private final ApplicationIdsProvider applicationIdsProvider;

	public DopplerLogStreamPublisher(
		CloudFoundryClient client,
		DopplerClient dopplerClient,
		ApplicationIdsProvider applicationIdsProvider
	) {
		this.client = client;
		this.dopplerClient = dopplerClient;
		this.applicationIdsProvider = applicationIdsProvider;
	}

	@Override
	public Flux<Envelope> getLogStream(String serviceInstanceId) {
		return this.applicationIdsProvider
			.getApplicationIds(serviceInstanceId)
			.doOnNext(id -> LOG.debug("Starting log streaming for app with ID {}", id))
			.flatMap(this::createApplicationStreamer);
	}

	protected Flux<Envelope> createApplicationStreamer(String applicationId) {
		return client.applicationsV2().get(GetApplicationRequest.builder().applicationId(applicationId).build())
			.map(response -> response.getEntity().getName())
			.flatMapMany(appName ->
				dopplerClient.stream(StreamRequest.builder().applicationId(applicationId).build())
					.map(envelope -> LoggingUtils.injectAppNameIntoLogSourceInstance(appName, envelope))
			);
	}

}
