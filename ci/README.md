#App Broker CI Pipeline

### Managing BOSH Lite on GCP

Currently CircleCI is used to run the acceptance tests. The system under test is a BOSH Lite, provisioned using [BOSH Bootloader](https://github.com/cloudfoundry/bosh-bootloader)

The BOSH Lite director can be targeted using following steps;

* Install bosh-cli and bbl
	```
	$ brew tap cloudfoundry/tap
	$ brew install bosh-cli
	$ brew install bbl
	```
* Setup BBL environment variables
	```
	source ci/.envrc
	"Check variables ACTIVE_BBL_ENV_LOCAL ACTIVE_BBL_ENV_CIRCLECI to see the current environment aliases"
	./scripts/manage-bosh-bbl-environment.sh generate-bbl-state-directory -b [Env alias eg. plymouth]
	cd bosh-env-[env-alias]/bbl-state
	bbl print-env > bbl-env.sh && source bbl-env.sh
	bosh deployments
	```

### Set the active environment to be used by CI
./scripts/manage-bosh-bbl-environment.sh set-active-environment -b [bbl-env-alias] -t [local/circleci]

### Running CI pipeline locally
CircleCI allows pipelines to be run locally, this is very useful development purposes

* Install circle command line tool to run local builds:
	```$bash
	curl -o /usr/local/bin/circleci https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci && \
	chmod +x /usr/local/bin/circleci
	```
* Run script to execute pipeline
`ci/scripts/run-local-ci-job.sh`

### Credentials
CircleCI uses environment variables to store credentials (these are stored inside
a Vault instance within the CircleCI server). Environment variables need to
be uploaded to circle so they can be used by the build.


### Building / Repaving a BOSH Lite environment

* Decide a new name for the environment. Currently the naming convention is towns and villages in Devon (see https://www.devonguide.com/gazetteer.htm) so for example `ENV_NAME=appbroker-exeter`
* Create a working directory *outside of version control* to create the environments
* ./ci/scripts/manage-bosh-bbl-environment.sh create-new-environment -b plymouth -i [internal IP cidr eg 10.0.3.0/24] -e
* Test the new CF install `cf api $ENV_NAME.cf-app.com --skip-ssl-validation`