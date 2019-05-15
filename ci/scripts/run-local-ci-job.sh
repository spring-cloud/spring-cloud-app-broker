#!/usr/bin/env bash
set -euo pipefail

readonly PROG_DIR=$(readlink -m $(dirname $0))
readonly CI_DIR=$(dirname "${PROG_DIR}")
readonly PROJECT_ROOT=$(dirname "${CI_DIR}")

validate(){
	[[ $(type circleci) ]] || (echo "circleci tool not installed" >&2 ; circle_usage ; exit 2)
}

circle_usage() {
    cat <<- EOF
Install circle command line tool to run local builds:
    "curl -o /usr/local/bin/circleci https://circle-downloads.s3.amazonaws.com/releases/build_agent_wrapper/circleci && \
    chmod +x /usr/local/bin/circleci"
EOF
}

run_circle_command(){
	cd "${PROJECT_ROOT}"
	circleci build
}

validate
run_circle_command