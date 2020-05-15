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

package org.springframework.cloud.appbroker.acceptance;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import reactor.netty.http.client.HttpClient;
import reactor.util.Logger;
import reactor.util.Loggers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.appbroker.acceptance.services.HelloCredentialCreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpCreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpDeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.acceptance.services.NoOpUpdateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.appbroker.service.CreateServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.DeleteServiceInstanceWorkflow;
import org.springframework.cloud.appbroker.service.UpdateServiceInstanceWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.credhub.core.CredHubProperties;
import org.springframework.credhub.core.ReactiveCredHubOperations;
import org.springframework.credhub.core.ReactiveCredHubTemplate;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

/**
 * A Spring Boot application for running acceptance tests
 */
@SpringBootApplication
public class AppBrokerApplication {

	private static final Logger LOG = Loggers.getLogger(AppBrokerApplication.class);

	/**
	 * main application entry point
	 *
	 * @param args the args
	 */
	public static void main(String[] args) {
		SpringApplication.run(AppBrokerApplication.class, args);
	}

	/**
	 * A no-op CreateServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceWorkflow createServiceInstanceWorkflow() {
		return new NoOpCreateServiceInstanceWorkflow();
	}

	/**
	 * A no-op UpdateServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public UpdateServiceInstanceWorkflow updateServiceInstanceWorkflow() {
		return new NoOpUpdateServiceInstanceWorkflow();
	}

	/**
	 * A no-op DeleteServiceInstanceWorkflow bean
	 *
	 * @return the bean
	 */
	@Bean
	public DeleteServiceInstanceWorkflow deleteServiceInstanceWorkflow() {
		return new NoOpDeleteServiceInstanceWorkflow();
	}

	/**
	 * A no-op ServiceInstanceBindingService bean
	 *
	 * @return the bean
	 */
	@Bean
	public CreateServiceInstanceAppBindingWorkflow createServiceInstanceAppBindingWorkflow() {
		return new HelloCredentialCreateServiceInstanceAppBindingWorkflow();
	}

	/**
	 * A temporary ReactiveCredHubOperation bean before spring-credhub fix the auto-configured one in the next release.
	 *
	 * @return the bean
	 */
	@Bean // TODO: remove manual configuration of ReactiveCredHubOperations with spring-credhub 2.1.1.RELEASE
	public ReactiveCredHubOperations mtlsCredHubOperations(@Value("${spring.credhub.url}") String runtimeCredHubUrl) {
		CredHubProperties credHubProperties = new CredHubProperties();
		credHubProperties.setUrl(runtimeCredHubUrl);

		return new ReactiveCredHubTemplate(credHubProperties, getConnector());
	}

	private ClientHttpConnector getConnector() {
		HttpClient httpClient = HttpClient.create();

		httpClient = httpClient.secure(sslContextSpec -> {
			try {
				sslContextSpec.sslContext(
					new JdkSslContext(SSLContext.getDefault(), true, null, IdentityCipherSuiteFilter.INSTANCE,
						null, ClientAuth.REQUIRE, null, false));
			}
			catch (NoSuchAlgorithmException e) {
				LOG.error("Failed to get default SSL context", e);
			}
		});
		return new ReactorClientHttpConnector(httpClient);
	}

}
