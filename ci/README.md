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
	lpass show --notes 7083089362665788436 > ci/.envrc
	source ci/.envrc
	./scripts/populate-secrets-from-environment.sh
	./scripts/fix-bbl-state-json-line-breaks.py
	cd bbl-bosh-lite-environment/bbl-state
	bbl print-env > bbl-env.sh && source bbl-env.sh
	bosh deployments
	```

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

#### Adding a new variable or credential
* Edit the note in LastPass, adding the new variable:
`lpass edit --notes 7083089362665788436 --sync=now`
* Source all the variables in your local environment
	```
	lpass show --notes 7083089362665788436 > ci/.envrc
	source ci/.envrc
	```
* Upload the variables to CircleCi: `ci/scripts/export-envvars-to-cirle.sh`

### Building / Repaving a BOSH Lite environment

* Decide a new name for the environment. Currently the naming convention is towns and villages in Devon (see https://www.devonguide.com/gazetteer.htm) so for example `ENV_NAME=appbroker-exeter`
* Create a working directory *outside of version control* to create the environments
* `cd $WORKING_DIR`
* `git clone https://github.com/cloudfoundry/bosh-bootloader` 
* `mkdir -p $ENV_NAME/bbl-state`
* `cd $ENV_NAME/bbl-state`
* `bbl plan --name $ENV_NAME`
* `cp -r ../../bosh-bootloader/plan-patches/bosh-lite-gcp/. .`
* `bbl up`
* `bosh update-runtime-config bosh-deployment/runtime-configs/dns.yml --name dns`
* `git clone https://github.com/cloudfoundry/cf-deployment`
* `bosh -d cf deploy cf-deployment/cf-deployment.yml -o cf-deployment/operations/bosh-lite.yml -v system_domain=$ENV_NAME.cf-app.com -n`
* To get the cf admin password: `credhub get --name /bosh-$ENV_NAME/cf/cf_admin_password`
* Create a new DNS entry in route53 ./app-broker-repo/ci/scripts/update-route53.sh [$ENV_NAME] [Director external IP from $WORKING_DIR/$ENV_NAME/bbl-state/vars/director-vars-file.yml]
* Test the new CF install `cf api $ENV_NAME.cf-app.com --skip-ssl-validation`
TODO: The above steps produce a working CF install. There are still some manual steps unfortunately but there are plans to automate this. 
Every credential in `bbl-state.json`, `director-vars-store.yml` and `jumpbox-vars-store.yml` needs to be added to the `.envrc` file in lastpass