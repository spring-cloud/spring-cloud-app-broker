#!/usr/bin/env bash

set -euo pipefail

build() {
  ./gradlew --no-daemon --no-parallel check
}

main() {
  pushd "app-broker" > /dev/null
    build
  popd > /dev/null
}

main
