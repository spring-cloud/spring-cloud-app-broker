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

build() {
  ./gradlew assemble -x test
}

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
  ./gradlew clean assemble check -PacceptanceTests -b spring-cloud-app-broker-acceptance-tests/build.gradle
}

main() {
  pushd "app-broker" > /dev/null
    build
    run_tests
  popd > /dev/null
}

main
