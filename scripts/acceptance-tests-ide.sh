#!/usr/bin/env bash

set -euo pipefail

readonly METADATA_FILE="${METADATA_FILE:?must be set}"

declare CF_API_HOST
readonly CF_USERNAME="admin"
declare CF_PASSWORD

readonly CLIENT_ID="admin"
declare CLIENT_SECRET

readonly DEFAULT_ORG="test"
readonly DEFAULT_SPACE="development"

DEPLOYMENT_DIRECTORY="$(mktemp -d)"
readonly DEPLOYMENT_DIRECTORY

discover_environment() {
  local env_name
  env_name=$(jq -r .name <"${METADATA_FILE}")

  eval "$(bbl print-env --metadata-file "${METADATA_FILE}")"

  CF_API_HOST="$(jq -r .cf.api_url <"${METADATA_FILE}")"
  CF_PASSWORD="$(credhub get -n "/bosh-${env_name}/cf/cf_admin_password" -q)"
  CLIENT_SECRET="$(credhub get -n "/bosh-${env_name}/cf/uaa_admin_client_secret" -q)"
}

prepare_cf_deployment() {
  pushd "$DEPLOYMENT_DIRECTORY" >/dev/null

  bosh --deployment cf manifest >manifest.yml

  cat <<EOF >ops.yml
- type: replace
  path: /instance_groups/name=diego-cell/vm_type
  value: large
- type: replace
  path: /instance_groups/name=router/vm_type
  value: large
- type: replace
  path: /instance_groups/name=api/vm_type
  value: large
EOF

  bosh --non-interactive --deployment cf deploy --ops-file ops.yml manifest.yml

  popd >/dev/null
}

prepare_cf() {
  local -r test_instances_org="$DEFAULT_ORG-instances"

  cf login -a "$CF_API_HOST" -u "$CF_USERNAME" -p "$CF_PASSWORD" -o system --skip-ssl-validation

  cf create-org "$DEFAULT_ORG"
  cf create-space "$DEFAULT_SPACE" -o "$DEFAULT_ORG"

  cf create-org "$test_instances_org"
  cf create-space "$DEFAULT_SPACE" -o "$test_instances_org"
}

print_values() {
  cat <<EOF
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST=${CF_API_HOST}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT=443
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME=${CF_USERNAME}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD=${CF_PASSWORD}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_ID=${CLIENT_ID}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_SECRET=${CLIENT_SECRET}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG=${DEFAULT_ORG}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE=${DEFAULT_SPACE}
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION=true
TESTS_BROKERAPPPATH=build/libs/spring-cloud-app-broker-acceptance-tests.jar
EOF
}

main() {
  discover_environment
  prepare_cf_deployment
  prepare_cf
  print_values
}

main
