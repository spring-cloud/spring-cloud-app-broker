#!/usr/bin/env bash
set -euo pipefail

[[ -z "${1:-}" ]] && printf "Must provide Max Queue Time in *minutes* as script argument\n" && exit 1
[[ -z "${CIRCLE_BUILD_NUM:-}" ]] && printf "skipping, not running on remote circleci environment\n" && exit 0
: ${CIRCLECI_API_KEY:?"Required Env Variable not found!"}
: ${CIRCLE_PROJECT_USERNAME:?"Required Env Variable not found!"}
: ${CIRCLE_PROJECT_REPONAME:?"Required Env Variable not found!"}
: ${CIRCLE_REPOSITORY_URL:?"Required Env Variable not found!"}

readonly MAX_TIME=${1}

wait_for_previous_builds_to_complete(){
	local max_time_seconds=$((MAX_TIME * 60))

	until [[ $(curl -s "https://circleci.com/api/v1.1/project/github/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}?circle-token=${CIRCLECI_API_KEY}&filter=running" \
		| jq 'min_by(.build_num) | .build_num') \
		-eq ${CIRCLE_BUILD_NUM} ]]; do

			sleep 10
			printf "."
			if [[ "${SECONDS}" -gt "${max_time_seconds}" ]]; then
				# After cancelling the build, wait some time to ensure the job has been cancelled otherwise
				# the script will exit and the job will be incorrectly marked success or failed
				printf "Max time exceeded waiting to reach front of queue, cancelling build\n"
				cancel_current_build
				sleep 20
				exit 1
			fi
	done
	printf "Build is front of queue\n"

}

cancel_current_build() {
	curl -s -X POST \
		"https://circleci.com/api/v1.1/project/github/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}/${CIRCLE_BUILD_NUM}/cancel?circle-token=${CIRCLECI_API_KEY}" > /dev/null
}

wait_for_previous_builds_to_complete