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
