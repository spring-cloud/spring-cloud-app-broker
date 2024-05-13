# App Broker CI Pipeline

## Running the acceptance tests on Cloud Foundry locally

With the following variables set:
```
API_HOST
API_PORT
USERNAME
PASSWORD
CLIENT_ID
CLIENT_SECRET
DEFAULT_ORG
DEFAULT_SPACE
SKIP_SSL_VALIDATION
```
Run script to execute the acceptance tests suite:

```$bash
ci/scripts/acceptance-tests.sh
```

## Releasing

A release pipeline inspired by the one created by the Spring boot team https://github.com/spring-projects/spring-boot/blob/master/ci/

It is composed by three different groups:

- The first group is a basic build so that we can always be sure which build we will be getting when releasing, followed by the acceptance tests.
- The second group are the releases.
- The last group is the CI image used by the different tasks and the source can be found here: `ci/images/release-ci-image/Dockerfile`.

### Releases

The original pipeline was decomposed into different jobs so that we could recover from each of them manually

### Fly

The pipeline can be set using the following script on the most recent branch of the repository:

```$bash
$ ./scripts/set-pipelines.sh
```

### Release commands

If you don't want to click, you can trigger each job using the CLI:

To release a milestone:

```$bash
$ fly -t app-broker trigger-job -j release-test/stage-milestone
$ fly -t app-broker trigger-job -j release-test/promote-milestone
```

To release an RC:

```$bash
$ fly -t app-broker trigger-job -j release-test/stage-rc
$ fly -t app-broker trigger-job -j release-test/promote-rc
```

To release a GA:

```$bash
$ fly -t app-broker trigger-job -j release-test/stage-release
$ fly -t app-broker trigger-job -j release-test/promote-release
$ fly -t app-broker trigger-job -j release-test/distribute-release
$ fly -t app-broker trigger-job -j release-test/sync-to-maven-central
```
