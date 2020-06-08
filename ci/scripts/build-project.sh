#!/bin/bash

set -euo pipefail

readonly ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE:-true}"

# shellcheck source=common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository

pushd git-repo >/dev/null
./gradlew --parallel clean build install \
	-PonlyShowStandardStreamsOnTestFailure="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE}" \
	-Dmaven.repo.local="${repository}"
popd >/dev/null
