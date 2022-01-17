#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET="app-broker"
readonly VERSION=1.5.x
readonly BRANCH=main
readonly CI_IMAGE_TAG=main

set_pipeline() {
	local pipeline_name pipeline_definition branch ci_image_tag
	pipeline_name="${1:?pipeline name must be provided}"
	pipeline_definition="${2:?pipeline definition file must be provided}"
	branch="${3:?branch must be provided}"
	ci_image_tag="${4:?ci_image_tag must be provided}"

	echo "Setting $pipeline_name pipeline..."
	fly --target "$FLY_TARGET" set-pipeline \
		--pipeline "$pipeline_name" \
		--config "$pipeline_definition" \
		--load-vars-from config-concourse.yml \
		--var "branch=$branch" \
		--var "ci-image-tag=$ci_image_tag"
}

main() {
	fly -t app-broker sync

	pushd "$(dirname "$0")/../ci" >/dev/null

	set_pipeline "app-broker-$VERSION"    pipeline.yml    "$BRANCH" "$CI_IMAGE_TAG"
	set_pipeline "app-broker-$VERSION-pr" pr-pipeline.yml "$BRANCH" "$CI_IMAGE_TAG"

	popd >/dev/null
}

main
