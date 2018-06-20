package org.springframework.cloud.appbroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.appbroker.serviceinstance.AppBrokerCreateInstanceProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppBrokerCreateInstanceProperties.class})
public class AppBrokerAutoConfigureApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppBrokerAutoConfigureApplication.class, args);
	}
}
