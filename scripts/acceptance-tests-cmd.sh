#!/bin/bash

set -euo pipefail

export TOOLSMITHS_ENVIRONMENT_NAME="${TOOLSMITHS_ENVIRONMENT_NAME:-${1?Set TOOLSMITHS_ENVIRONMENT_NAME or pass environment name as first argument}}"

readonly TOOLSMITH_METADATA_FILE="$(mktemp)"

trap cleanup EXIT

cleanup() {
  rm -f "$TOOLSMITH_METADATA_FILE"
}

smith cat > "$TOOLSMITH_METADATA_FILE"
eval "$(bbl print-env --metadata-file "$TOOLSMITH_METADATA_FILE")"
api_host="$(jq -r .cf.api_url < "$TOOLSMITH_METADATA_FILE")"
cloudfoundry_password="$(credhub get -n "/bosh-${TOOLSMITHS_ENVIRONMENT_NAME}/cf/cf_admin_password" -q)"
cloudfoundry_client_secret="$(credhub get -n "/bosh-${TOOLSMITHS_ENVIRONMENT_NAME}/cf/uaa_admin_client_secret" -q)"

cat << EOF
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST=$api_host \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT=443 \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION=true \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME=admin \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD=$cloudfoundry_password \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_ID=admin \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_CLIENT_SECRET=$cloudfoundry_client_secret \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG=test \\
SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE=development \\
TESTS_BROKERAPPPATH=build/libs/spring-cloud-app-broker-acceptance-tests.jar \\
./gradlew -PacceptanceTests :spring-cloud-app-broker-acceptance-tests:test
EOF
