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

package org.springframework.cloud.appbroker.logging.streaming;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import org.springframework.cloud.appbroker.logging.streaming.events.ServiceInstanceLogEvent;
import org.springframework.cloud.appbroker.logging.streaming.events.ServiceInstanceLoggingEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;

public class ApplicationLogStreamPublisher implements ApplicationListener<ServiceInstanceLoggingEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationLogStreamPublisher.class);

	private final Map<String, Registration> registry = new HashMap<>();

	private final LogStreamPublisher<org.cloudfoundry.dropsonde.events.Envelope> logStreamPublisher;

	private final ApplicationEventPublisher publisher;

	public ApplicationLogStreamPublisher(
		LogStreamPublisher<org.cloudfoundry.dropsonde.events.Envelope> logStreamPublisher,
		ApplicationEventPublisher publisher) {
		this.logStreamPublisher = logStreamPublisher;
		this.publisher = publisher;
	}

	@Override
	public void onApplicationEvent(ServiceInstanceLoggingEvent event) {
		final String serviceInstanceId = event.getServiceInstanceId();
		switch (event.getOperation()) {
			case START:
				LOG.debug("Received event to begin listening to logs for {}", serviceInstanceId);
				this.startPublishing(serviceInstanceId);
				return;
			case STOP:
				LOG.debug("Received event to stop listening to logs for {}", serviceInstanceId);
				this.stopPublishing(serviceInstanceId);
				return;

			default:
				throw new IllegalArgumentException("Unknown operation: " + event.getOperation());
		}
	}

	private void startPublishing(String serviceInstanceId) {
		registry.compute(serviceInstanceId, (key, existingRegistration) -> {
			if (existingRegistration != null) {
				LOG.debug("Incrementing registration subscription count for {}", serviceInstanceId);
				existingRegistration.increment();
				return existingRegistration;
			}

			Flux<org.cloudfoundry.dropsonde.events.Envelope> logStream = this.logStreamPublisher
				.getLogStream(serviceInstanceId);

			final Disposable subscription = logStream
				.doOnNext(
					envelope -> publisher
						.publishEvent(new ServiceInstanceLogEvent(this, serviceInstanceId, envelope)))
				.subscribe();

			LOG.debug("Creating new registration for {}", serviceInstanceId);
			return new Registration(subscription);
		});
	}

	private void stopPublishing(String serviceInstanceId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Received event to stop publishing logs for {}", serviceInstanceId);
		}

		registry.compute(serviceInstanceId, (key, registration) -> {
			if (registration == null) {
				if (LOG.isWarnEnabled()) {
					LOG.warn("Received deregister event for service instance {} but there no event handler registered",
						serviceInstanceId);
				}
				return null;
			}

			if (registration.decrement() == 0) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Disposing of registration since there are no more subscriptions");
				}
				registration.getSubscription().dispose();
				return null;
			}

			return registration;
		});
	}

	private final static class Registration {

		private final Disposable subscription;

		private int count = 1;

		private Registration(Disposable subscription) {
			this.subscription = subscription;
		}

		public void increment() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Incrementing subscription count from {} to {}", count, count + 1);
			}

			++count;
		}

		public int decrement() {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Decrementing subscription count from {} to {}", count, count - 1);
			}

			return --count;
		}

		public Disposable getSubscription() {
			return subscription;
		}

	}

}
