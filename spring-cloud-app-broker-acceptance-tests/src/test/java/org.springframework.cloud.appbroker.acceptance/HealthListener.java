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

package org.springframework.cloud.appbroker.acceptance;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class HealthListener {

	private final AtomicInteger requests = new AtomicInteger();
	private final AtomicInteger errors = new AtomicInteger();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private Thread runner;
	private final RestTemplate restTemplate;

	public HealthListener(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	void start(String path) {
		if (running.get()) {
			throw new IllegalStateException("cannot start when test is already running");
		}
		requests.set(0);
		errors.set(0);
		running.set(true);

		runner = new Thread(() -> {
			while (running.get()) {
				try {
					requests.incrementAndGet();
					ResponseEntity<String> response = restTemplate.getForEntity(URI.create("http://" + path + "/actuator/health"), String.class);
					if (response.getStatusCodeValue() != 200) {
						errors.incrementAndGet();
					}
					Thread.sleep(1000);
				}
				catch (RestClientException | InterruptedException re) {
					errors.incrementAndGet();
				}
			}
		});
		runner.start();
	}

	void stop() {
		running.set(false);
		try {
			runner.join();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	int getSuccesses() {
		return requests.get();
	}

	int getFailures() {
		return errors.get();
	}

}
