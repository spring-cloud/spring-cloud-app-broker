#!/usr/bin/env bash

set -euo pipefail

readonly FLY_TARGET="${FLY_TARGET:-"app-broker"}"
readonly PIPELINE_NAME_SUFFIX="${PIPELINE_NAME_SUFFIX:-""}"
readonly PIPELINE_TYPE=${1:-""}

[[ "$PIPELINE_NAME_SUFFIX" ]] && DISABLE_SLACK_ALERTING="true" || DISABLE_SLACK_ALERTING="false"

set_branch_pipeline() {
  local -r pipeline_name="app-broker${PIPELINE_NAME_SUFFIX:+"-$PIPELINE_NAME_SUFFIX"}"
  local -r branches=("2.2.x" "2.1.x" "2.0.x" "1.6.x" "1.5.x")

	for branch in "${branches[@]}"; do
  	echo "Setting $pipeline_name $branch pipeline..."

    fly --target "$FLY_TARGET" set-pipeline \
      --pipeline "$pipeline_name" \
      --config pipeline.yml \
      --load-vars-from config-concourse.yml \
      --instance-var "branch=$branch" \
      --var "ci-image-tag=$branch" \
      --var "disable-slack-alerting=$DISABLE_SLACK_ALERTING" \`
  done
}

set_pr_manager_pipeline() {
	local -r pipeline_name="app-broker-pull-requests${PIPELINE_NAME_SUFFIX:+"-$PIPELINE_NAME_SUFFIX"}"
	local -r branch="2.2.x"

	echo "Setting PR manager pipeline..."

	fly --target "$FLY_TARGET" set-pipeline --pipeline "$pipeline_name" \
		--config pr-manager-pipeline.yml \
		--load-vars-from config-concourse.yml \
		--var ci-image-tag="$branch" \
		--var "branch=$branch" \
		--var pipeline-name="$pipeline_name"
}

set_pr_pipeline() {
	local -r pr_number="${1:?first argument must be the PR number}"
	local -r pipeline_name="app-broker-pr-$pr_number${PIPELINE_NAME_SUFFIX:+"-$PIPELINE_NAME_SUFFIX"}"

	echo "Setting PR pipeline for PR $pr_number..."

	local -r pr_info="$(gh pr view "$pr_number" --json baseRefName,headRefOid)"

	fly --target "$FLY_TARGET" set-pipeline --pipeline "$pipeline_name" \
		--config pr-pipeline.yml \
		--load-vars-from config-concourse.yml \
		--var ci-image-tag="pr-test" \
		--var pipeline-name="$pipeline_name" \
		--var pr.number="$pr_number" \
		--var pr.base-branch="$(jq -r '.baseRefName' <<< "$pr_info")" \
		--var pr.commit="$(jq -r '.headRefOid' <<< "$pr_info")"
}

main() {
	pushd "$(dirname "$0")/../ci" >/dev/null

	case "${PIPELINE_TYPE#--}" in
		"branch")
			set_branch_pipeline
			;;
		"pr-manager")
			set_pr_manager_pipeline
			;;
		"pr")
			set_pr_pipeline "$2"
			;;
		*)
			set_branch_pipeline
			set_pr_manager_pipeline
			;;
	esac

	popd >/dev/null
}

main "$@"
