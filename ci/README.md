#App Broker CI Pipeline

### Running CI pipeline locally
CircleCI allows pipelines to be run locally, this is very useful development purposes

* Install circle command line tool to run local builds:
	```$bash
	curl -o /usr/local/bin/circleci https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci && \
	chmod +x /usr/local/bin/circleci
	```
* Run script to execute pipeline
`ci/scripts/run-local-ci-job.sh`
