#!/bin/bash
set -euo pipefail

readonly PROG_DIR=$(readlink -m $(dirname $0))
readonly SCRIPTS_DIR=$(dirname "${PROG_DIR}")
readonly CI_DIR=$(dirname "${SCRIPTS_DIR}")
cd "${SCRIPTS_DIR}/bbl-bosh-lite-environment/bbl-state/"

substitute_bbl_state_file(){

	perl 	-pe "s|<%= bblEnvId %>|${BBL_ENV_ID}|;" \
			-pe "s|<%= jumpboxUrl %>|${JUMPBOX_URL}|;" \
			-pe "s|<%= directorUsername %>|${DIRECTOR_USERNAME}|;" \
			-pe "s|<%= directorPassword %>|${DIRECTOR_PASSWORD}|;" \
			-pe "s|<%= directorInternalIp %>|${DIRECTOR_INTERNAL_IP}|;" \
			-pe "s|<%= directorPort %>|${DIRECTOR_PORT}|;" \
			-pe "s|<%= directorSSLCA %>|$(echo ${DIRECTOR_SSLCA} | jq -aR .)|;" \
			-pe "s|<%= directorSSLCertificate %>|$(echo ${DIRECTOR_SSLCERTIFICATE} | jq -aR .)|;" \
			-pe "s|<%= directorSSLPrivateKey %>|$(echo ${DIRECTOR_SSLPRIVATE_KEY} | jq -aR .)|;" \
			bbl-state.json.tmpl > bbl-state.json
}

substitute_director_vars_store(){
	perl 	 -pe "s|<%= directorAdminPassword %>|${DIRECTOR_PASSWORD}|;" \
			 -pe "s|<%= credhubAdminClientSecret %>|${CREDHUB_ADMIN_CLIENT_SECRET}|;" \
			 -pe "s|<%= credhubCA %>|${CREDHUB_CA}|;" \
			 -pe "s|<%= uaaCert %>|${UAA_CERT}|;" \
			 -pe "s|<%= directorJumpboxSshPrivateKey %>|${DIRECTOR_JUMPBOX_SSH_PRIVATE_KEY}|;" \
			 director-vars-store.yml.tmpl > vars/director-vars-store.yml
}

substitute_director_vars_file(){
	perl 	-pe "s|<%= directorInternalIp %>|${DIRECTOR_INTERNAL_IP}|;" \
			 director-vars-file.yml.tmpl > vars/director-vars-file.yml
}

substitute_jumpbox_vars_store(){
	perl 	-pe "s|<%= jumpboxPrivateKey %>|${JUMPBOX_PRIVATE_KEY}|;" \
			 jumpbox-vars-store.yml.tmpl > vars/jumpbox-vars-store.yml
}

substitute_bbl_state_file
substitute_director_vars_store
substitute_director_vars_file
substitute_jumpbox_vars_store