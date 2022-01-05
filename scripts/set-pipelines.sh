#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET="app-broker"
readonly VERSION=1.4.x
readonly BRANCH=main

set_pipeline() {
	local pipeline_name pipeline_definition branch ci_image_tag
	pipeline_name="${1:?pipeline name must be provided}"
	pipeline_definition="${2:?pipeline definition file must be provided}"
	branch="${3:?branch must be provided}"
	ci_image_tag="${4:-$branch}"

	echo "Setting $pipeline_name pipeline..."
	fly --target "$FLY_TARGET" set-pipeline \
		--pipeline "$pipeline_name" \
		--config "$pipeline_definition" \
		--load-vars-from config-concourse.yml \
		--var "branch=$branch"
}

main() {
	fly -t app-broker sync

	pushd "$(dirname "$0")/../ci" >/dev/null

	set_pipeline "app-broker-$VERSION"    pipeline.yml    "$BRANCH"
	set_pipeline "app-broker-$VERSION-pr" pr-pipeline.yml "$BRANCH"

	popd >/dev/null
}

main
