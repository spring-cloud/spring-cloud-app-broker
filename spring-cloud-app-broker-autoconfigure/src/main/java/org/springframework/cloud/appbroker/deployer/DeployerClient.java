package org.springframework.cloud.appbroker.deployer;

import reactor.core.publisher.Mono;

import org.springframework.stereotype.Component;

//TODO This should be in the App Broker core subproject
// TODO I should not be public in order to avoid usage outside of it's bounded context (app deployment)
@Component
/**
 * The deployer client exists to provide an adapter / anti-corruption layer between the platform deployer component {@see ReactiveAppDeployer}
 * and App Broker workflow components. For rationale {@see https://docs.microsoft.com/en-us/azure/architecture/patterns/anti-corruption-layer}
 *
 * A simple rule we have been following is;
 * Controller --calls--> Service ✅
 * Service    --calls--> Client  ✅
 * Service    --calls--> Service ⛔️ probably a tight coupling between two bounded contexts
 */
public class DeployerClient {


	// TODO I should not be public in order to avoid usage outside of it's bounded context (app deployment)
	public Mono<String> deploy(DeployerApplication deployerApplication) {

		return Mono.just("running");
	}

}
