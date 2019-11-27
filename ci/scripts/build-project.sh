#!/bin/bash
set -e

# shellcheck source=scripts/common.sh
source "$(dirname "$0")/common.sh"
repository=$(pwd)/distribution-repository

pushd git-repo >/dev/null
./gradlew --no-daemon clean build install -Dmaven.repo.local="${repository}" -Dorg.gradle.jvmargs="-Xmx512m -Xmx2048m"
popd >/dev/null
