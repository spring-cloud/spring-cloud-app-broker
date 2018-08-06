#!/usr/bin/env bash
# The BOSH director might be in use by someone else. Check `bosh locks` command before running ATs
set -euo pipefail

[[ -z "${1:-}" ]] && printf "Must provide Max Queue Time in *minutes* as script argument\n" && exit 1

readonly MAX_TIME=${1}

wait_for_bosh_locks_to_be_released(){
	local max_time_seconds=$((MAX_TIME * 60))

	until [[ $(bosh locks --json | jq '.Tables[].Rows | length') -eq 0 ]]; do

			sleep 10
			printf "."
			if [[ "${SECONDS}" -gt "${max_time_seconds}" ]]; then
				# After cancelling the build, wait some time to ensure the job has been cancelled otherwise
				# the script will exit and the job will be incorrectly marked success or failed
				printf "Max time exceeded waiting for bosh locks to be released, cancelling build\n"
				cancel_current_build
				sleep 20
				exit 1
			fi
	done
	printf "No BOSH locks found, continuing build\n"
}

cancel_current_build() {
	curl -s -X POST \
		"https://circleci.com/api/v1.1/project/github/${CIRCLE_PROJECT_USERNAME}/${CIRCLE_PROJECT_REPONAME}/${CIRCLE_BUILD_NUM}/cancel?circle-token=${CIRCLECI_API_KEY}" > /dev/null
}

wait_for_bosh_locks_to_be_released