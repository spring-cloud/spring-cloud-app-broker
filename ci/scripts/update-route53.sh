#!/usr/bin/env bash
# After creating a new environment with BBL you need to create / update the DNS zone in route53
set -euo pipefail

readonly PROG_DIR=$(readlink -m $(dirname $0))
readonly CI_DIR=$(dirname "${PROG_DIR}")
[[ -z "${1:-}" ]] && printf "Must provide BBL environment id (use bbl env-id to retrieve)\n" && exit 1
[[ -z "${2:-}" ]] && printf "Must provide external IP of Director VM (found in file bbl-state/vars/director-vars-file.yml)\n" && exit 1

validate(){
	[[ $(type aws) ]] || (echo "aws-cli tool not installed" >&2 ; aws_usage ; exit 2)
	[[ -f "${CI_DIR}/.envrc" ]] || (printf "Download .envrc in directory ${CI_DIR} before running script"; exit 1)

}

aws_usage() {
    cat <<- EOF
Install aws-cli tool using "brew install aws-cli"
EOF
}

update_dns() {
	local bbl_env_id="${1}" director_external_ip="${2}"
	source "${CI_DIR}/.envrc"
	[[ -z "${DNS_ZONE_ID:-}" ]] && printf "DNS_ZONE_ID must be set" && exit 1

	aws route53 change-resource-record-sets --hosted-zone-id /hostedzone/"${DNS_ZONE_ID}" --change-batch='{
			"Comment": "",
			"Changes": [
					{
							"Action": "UPSERT",
							"ResourceRecordSet": {
									"Name": "*.'"${bbl_env_id}"'.cf-app.com",
									"Type": "A",
									"TTL": 0,
									"ResourceRecords": [
											{
													"Value": "'"${director_external_ip}"'"
											}
									]
							}
					}
			]
	}'

}
validate
update_dns "${1}" "${2}"
