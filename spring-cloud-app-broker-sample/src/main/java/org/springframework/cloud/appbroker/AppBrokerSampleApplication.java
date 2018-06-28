package org.springframework.cloud.appbroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.deployer.spi.cloudfoundry.CloudFoundryDeployerAutoConfiguration;

@SpringBootApplication(exclude = CloudFoundryDeployerAutoConfiguration.class)
public class AppBrokerSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppBrokerSampleApplication.class, args);
	}
}
