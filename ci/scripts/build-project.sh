#!/bin/bash
set -e

readonly ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE:"true"}"

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository

pushd git-repo >/dev/null
./gradlew --no-daemon clean build install \
	-PonlyShowStandardStreamsOnTestFailure="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE}" \
	-Dmaven.repo.local="${repository}" \
	-Dorg.gradle.jvmargs="-Xmx512m -Xmx2048m"
popd >/dev/null
