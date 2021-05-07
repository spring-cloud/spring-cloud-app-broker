#!/usr/bin/env bash

set -euo pipefail

readonly TOOLSMITH_ENV_INPUT="${TOOLSMITH_ENV_INPUT:?must be set}"
readonly DEFAULT_ORG="${DEFAULT_ORG:?must be set}"
readonly DEFAULT_SPACE="${DEFAULT_SPACE:?must be set}"
readonly SKIP_SSL_VALIDATION="${SKIP_SSL_VALIDATION:?must be set}"
readonly ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE:-true}"

declare API_HOST
readonly API_PORT=443
readonly USERNAME="admin"
declare PASSWORD
readonly CLIENT_ID="admin"
declare CLIENT_SECRET

discover_environment() {
  local env_name
  env_name=$(cat "$TOOLSMITH_ENV_INPUT/name")

  eval "$(bbl print-env --metadata-file "$TOOLSMITH_ENV_INPUT/metadata")"

  credhub login \
    --client-name="$(jq -r .bosh.credhub_client < "$TOOLSMITH_ENV_INPUT/metadata")" \
    --client-secret="$(jq -r .bosh.credhub_secret < "$TOOLSMITH_ENV_INPUT/metadata")" \
    --server="$(jq -r .bosh.credhub_server < "$TOOLSMITH_ENV_INPUT/metadata")" \
    --ca-cert="$(jq -r .bosh.credhub_ca_cert < "$TOOLSMITH_ENV_INPUT/metadata")"

  API_HOST="$(jq -r .cf.api_url < "$TOOLSMITH_ENV_INPUT/metadata")"
  PASSWORD="$(credhub get -n "/bosh-${env_name}/cf/cf_admin_password" -q)"
  CLIENT_SECRET="$(credhub get -n "/bosh-${env_name}/cf/uaa_admin_client_secret" -q)"
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
  ./gradlew -PacceptanceTests \
  	-PonlyShowStandardStreamsOnTestFailure="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE}" \
  	:spring-cloud-app-broker-acceptance-tests:test
}

main() {
  discover_environment

  echo "Running tests against $API_HOST"
  echo

  pushd "git-repo" > /dev/null
    run_tests
  popd > /dev/null
}

main
