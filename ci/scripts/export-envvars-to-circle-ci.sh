#!/usr/bin/env bash
set -euo pipefail
readonly CIRCLE_API_BASE=https://circleci.com/api/v1.1/project/github/spring-cloud-incubator/spring-cloud-app-broker
readonly PROG_DIR=$(readlink -m $(dirname $0))
readonly CI_DIR=$(dirname "${PROG_DIR}")

export_variables_to_circle() {
	[[ ! -f "${CI_DIR}/.envrc" ]] && printf "Download .envrc from LastPass using \"lpass show --notes 7083089362665788436\" in directory ${SCRIPTS_DIR} before running script" && exit 1

    : ${DIRECTOR_SSLCA:?"Required Env Variable not found!"}
    : ${JUMPBOX_PRIVATE_KEY:?"Required Env Variable not found!"}
    : ${UAA_CERT:?"Required Env Variable not found!"}
    : ${CREDHUB_CA:?"Required Env Variable not found!"}
    : ${LB_KEY:?"Required Env Variable not found!"}
    : ${LB_CERT:?"Required Env Variable not found!"}
    : ${DIRECTOR_SSLPRIVATE_KEY:?"Required Env Variable not found!"}
    : ${DIRECTOR_SSLCERTIFICATE:?"Required Env Variable not found!"}
    : ${DIRECTOR_SSLCERTIFICATE:?"Required Env Variable not found!"}
    : ${DIRECTOR_JUMPBOX_SSH_PRIVATE_KEY:?"Required Env Variable not found!"}
    : ${DIRECTOR_USERNAME:?"Required Env Variable not found!"}
    : ${DIRECTOR_PASSWORD:?"Required Env Variable not found!"}
    : ${DIRECTOR_INTERNAL_IP:?"Required Env Variable not found!"}
    : ${DIRECTOR_PORT:?"Required Env Variable not found!"}
    : ${JUMPBOX_URL:?"Required Env Variable not found!"}
    : ${LB_DOMAIN:?"Required Env Variable not found!"}
    : ${DIRECTOR_ADMIN_PASSWORD:?"Required Env Variable not found!"}
    : ${CREDHUB_ADMIN_CLIENT_SECRET:?"Required Env Variable not found!"}
    : ${BBL_ENV_ID:?"Required Env Variable not found!"}
    : ${CIRCLECI_API_KEY:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE:?"Required Env Variable not found!"}
    : ${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION:?"Required Env Variable not found!"}
    : ${TESTS_SAMPLEBROKERAPPPATH:?"Required Env Variable not found!"}

	create_envvar_in_circle DIRECTOR_SSLCA "${DIRECTOR_SSLCA}"
	create_envvar_in_circle JUMPBOX_PRIVATE_KEY "${JUMPBOX_PRIVATE_KEY}"
	create_envvar_in_circle UAA_CERT "${UAA_CERT}"
	create_envvar_in_circle CREDHUB_CA "${CREDHUB_CA}"
	create_envvar_in_circle LB_KEY "${LB_KEY}"
	create_envvar_in_circle LB_CERT "${LB_CERT}"
	create_envvar_in_circle DIRECTOR_SSLPRIVATE_KEY "${DIRECTOR_SSLPRIVATE_KEY}"
	create_envvar_in_circle DIRECTOR_SSLCERTIFICATE "${DIRECTOR_SSLCERTIFICATE}"
	create_envvar_in_circle DIRECTOR_JUMPBOX_SSH_PRIVATE_KEY "${DIRECTOR_JUMPBOX_SSH_PRIVATE_KEY}"
	create_envvar_in_circle DIRECTOR_USERNAME "${DIRECTOR_USERNAME}"
	create_envvar_in_circle DIRECTOR_PASSWORD "${DIRECTOR_PASSWORD}"
	create_envvar_in_circle DIRECTOR_INTERNAL_IP "${DIRECTOR_INTERNAL_IP}"
	create_envvar_in_circle DIRECTOR_PORT "${DIRECTOR_PORT}"
	create_envvar_in_circle JUMPBOX_URL "${JUMPBOX_URL}"
	create_envvar_in_circle LB_DOMAIN "${LB_DOMAIN}"
	create_envvar_in_circle DIRECTOR_ADMIN_PASSWORD "${DIRECTOR_ADMIN_PASSWORD}"
	create_envvar_in_circle CREDHUB_ADMIN_CLIENT_SECRET "${CREDHUB_ADMIN_CLIENT_SECRET}"
	create_envvar_in_circle BBL_ENV_ID "${BBL_ENV_ID}"
	create_envvar_in_circle CIRCLECI_API_KEY "${CIRCLECI_API_KEY}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_PORT}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_USERNAME}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_ORG}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_DEFAULT_SPACE}"
	create_envvar_in_circle SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION "${SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_SKIP_SSL_VALIDATION}"
	create_envvar_in_circle TESTS_SAMPLEBROKERAPPPATH "${TESTS_SAMPLEBROKERAPPPATH}"

}

create_envvar_in_circle() {
	local envvar_name=$1
	local envvar_value=$2

	curl -XPOST "${CIRCLE_API_BASE}/envvar?circle-token=${CIRCLECI_API_KEY}" \
		-H "Content-Type: application/json" -d '{"name": "'"${envvar_name}"'", "value": "'"${envvar_value}"'"}'
}

export_variables_to_circle