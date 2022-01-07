#!/bin/bash

set -euo pipefail

readonly ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE:-true}"
readonly SKIP_TESTS="${SKIP_TESTS:-false}"
readonly DISTRIBUTION_REPOSITORY_OUTPUT="${DISTRIBUTION_REPOSITORY_OUTPUT:?must be set}"

if [ "$SKIP_TESTS" == "true" ]; then
	build_task=assemble
else
	build_task=build
fi

./gradlew --parallel clean "$build_task" publish \
	-PonlyShowStandardStreamsOnTestFailure="${ONLY_SHOW_STANDARD_STREAMS_ON_TEST_FAILURE}" \
	-PpublicationRepository="${DISTRIBUTION_REPOSITORY_OUTPUT}"
