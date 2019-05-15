#!/usr/bin/env bash

set -euo pipefail

readonly SCS_SECRETS_LAST_PASS_ID="1938546022346916823"
readonly SCS_ENVIRONMENT_LAST_PASS_ID="4685111791412820196"

secrets_file=$(mktemp).yml

fetch_secrets() {
  lpass show --notes "${SCS_ENVIRONMENT_LAST_PASS_ID}" > "${secrets_file}"
  lpass show --notes "${SCS_SECRETS_LAST_PASS_ID}" >> "${secrets_file}"
}

set_app_broker_pipeline() {
  echo "Setting app-broker-1.0.x pipeline..."
  fly -t scs set-pipeline -p app-broker-1.0.x -c pipeline.yml -l config-concourse.yml -l "${secrets_file}"
}

cleanup() {
  rm "${secrets_file}"
}

trap "cleanup" EXIT

main() {
  fly -t scs sync

  pushd "$(dirname $0)/.." > /dev/null
    fetch_secrets
    set_app_broker_pipeline
  popd > /dev/null
}

main
