#!/usr/bin/env bash

set -euo pipefail

readonly API_HOST="${API_HOST:?must be set}"
readonly API_PORT="${API_PORT:?must be set}"
readonly USERNAME="${USERNAME:?must be set}"
readonly PASSWORD="${PASSWORD:?must be set}"
readonly CLIENT_ID="${CLIENT_ID:?must be set}"
readonly CLIENT_SECRET="${CLIENT_SECRET:?must be set}"
readonly DEFAULT_ORG="${DEFAULT_ORG:?must be set}"
readonly DEFAULT_SPACE="${DEFAULT_SPACE:?must be set}"
readonly SKIP_SSL_VALIDATION="${SKIP_SSL_VALIDATION:?must be set}"
readonly ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE:-true}"

run_tests() {
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST="${API_HOST}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT="${API_PORT}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME="${USERNAME}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD="${PASSWORD}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_ID="${CLIENT_ID}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_SECRET="${CLIENT_SECRET}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG="${DEFAULT_ORG}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE="${DEFAULT_SPACE}"
  export SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION="${SKIP_SSL_VALIDATION}"
  export TESTS_BROKERAPPPATH=build/libs/spring-cloud-app-broker-acceptance-tests.jar
  ./gradlew -PacceptanceTests \
  	-PonlyShowStandardStreamsOnTestFailure="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE}" \
  	:spring-cloud-app-broker-acceptance-tests:test
}

main() {
  echo "Running tests against $API_HOST"
  echo

  pushd "git-repo" > /dev/null
    run_tests
  popd > /dev/null
}

main
