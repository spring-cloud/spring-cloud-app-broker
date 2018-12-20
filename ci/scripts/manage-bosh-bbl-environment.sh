#!/usr/bin/env bash
set -euo pipefail
readonly SCRIPT_DIR=$(readlink -m $(dirname $0))
readonly CI_DIR=$(dirname "${SCRIPT_DIR}")
readonly BBL_VERSION=v6.9.16
readonly BOSH_VERSION=5.2.2
readonly CREDHUB_VERSION=2.0.0
readonly BBL_GCP_SERVICE_ACCOUNT_KEY="${CI_DIR}/gcp-key.json"
readonly CIRCLE_API_BASE=https://circleci.com/api/v1.1/project/github/spring-cloud-incubator/spring-cloud-app-broker

validate(){
	local -r internal_cidr="${1}"
	source_environment_variables
	[[ $(type bbl) ]] || (echo "bbl tool not installed" >&2 ; bbl_usage ; exit 2)
	[[ $(type gcloud) ]] || (echo "gcloud tool not installed" >&2 ; gcloud_usage ; exit 2)
	[[ $(type bosh) ]] || (echo "bosh cli not installed" >&2 ; bosh_usage ; exit 2)
	[[ $(type credhub) ]] || (echo "credhub cli not installed" >&2 ; credhub_usage ; exit 2)
	[[ $(type aws) ]] || (echo "aws-cli tool not installed, Install using \"brew install aws-cli\"" >&2 ; exit 2)

	has_ip_cidr_already_been_used "${internal_cidr}"
	is_ip_cidr_valid "${internal_cidr}"
}

bbl_usage() {
    cat <<- EOF
Install bbl command line tool :
    "curl -o /usr/local/bin/bbl https://github.com/cloudfoundry/bosh-bootloader/releases/download/${BBL_VERSION}/bbl-${BBL_VERSION}_osx && \
    chmod +x /usr/local/bin/bbl"
EOF
}

bosh_usage() {
    cat <<- EOF
Install bosh command line tool:
    "curl -o /usr/local/bin/bosh https://github.com/cloudfoundry/bosh-cli/releases/download/${BOSH_VERSION}/bosh-cli-${BOSH_VERSION}-darwin-amd64 && \
    chmod +x /usr/local/bin/bosh"
EOF
}

gcloud_usage() {
    cat <<- EOF
Install Google Cloud SDK, see https://cloud.google.com/sdk/docs/quickstart-macos
EOF
}

credhub_usage() {
	cat <<-EOF
Install Credhub CLI:
	"curl https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/${CREDHUB_VERSION}/credhub-darwin-${CREDHUB_VERSION}.tgz
	EOF
}

usage() {
    cat <<- EOF
Usage: ./manage-bosh-bbl-env.sh

	create-new-environment -e -l -b "env-suffix"  -i "internal cidr"
		"Creates a new bosh lite environment in GCP using BBL. Optionally upload state and variable data to Circleci and Lastpass"
		-e Flag to upload environment variables to CircleCI
		-i CIDR range for internal network of Jumpbox and BOSH director (must not have already been used in GCP project) eg 10.0.1.0/24
		-b Alias for new BBL environment that will be used for creating dns entry. Convention is an English town name, see https://en.wikipedia.org/wiki/List_of_towns_in_England

	generate-bbl-state-directory -b "env-suffix"
		"Generates a new BBL state directory based on an existing environment, using state and vars files that are base64 encoded in environment variables"
		-b Existing BBL environment that state directory will be generated for

	set-active-environment
		"Select which bosh lite environment will be used for cirleci or local ci testing"
		-t Target to set active, must equal "circleci" or "local"
		-b BBL environment alias to set as target

	delete-environment
		"Deletes a bosh lite environment and removes any variables"
		-b BBL environment alias to remove
EOF
}

has_ip_cidr_already_been_used(){
	local -r internal_cidr="${1}"
	echo "${GCP_KEY_BASE64}" | base64 -d > "${CI_DIR}/gcp-key.json"
	gcloud auth activate-service-account --key-file="${BBL_GCP_SERVICE_ACCOUNT_KEY}"
	gcloud config set project "${BBL_GCP_PROJECT_ID}"
	gcloud compute networks subnets list --filter="name :(appbroker*)" --format=json \
	| jq -e --arg IP_CIDR "${internal_cidr}" '. | map(.ipCidrRange) | index($IP_CIDR) | not'
}

is_ip_cidr_valid(){
	local -r internal_cidr="${1}"
	python3  -c "import ipaddress, sys; ipaddress.ip_network(sys.argv[1])" "${internal_cidr}"
}

calculate_jumpbox_ip(){
	local -r internal_cidr="${1}"
	python3  -c "import ipaddress, sys; n = ipaddress.ip_network(sys.argv[1]); print(n[5])" "${internal_cidr}"
}

calculate_director_ip(){
	local -r internal_cidr="${1}"
	python3  -c "import ipaddress, sys; n = ipaddress.ip_network(sys.argv[1]); print(n[6])" "${internal_cidr}"
}

calculate_gateway_ip(){
	local -r internal_cidr="${1}"
	python3  -c "import ipaddress, sys; n = ipaddress.ip_network(sys.argv[1]); print(n[1])" "${internal_cidr}"
}

source_environment_variables(){
	[[ ! -f "${CI_DIR}/.envrc" ]] && printf "Ensure .envrc is present in directory ${CI_DIR} before running script" && exit 1
	source "${CI_DIR}/.envrc"
}

create_bbl_state_dir() {
	local -r bbl_env_suffix="${1}"
	mkdir -p $(get_bbl_state_dir "${bbl_env_suffix}")
}

create_new_bbl_plan() {
	local -r bbl_env_suffix="${1}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	local -r bbl_env_name="${bbl_env_suffix}"

	bbl plan --name "${bbl_env_suffix}" -s "${bbl_state_dir}" --gcp-service-account-key="${CI_DIR}/gcp-key.json"
}

copy_plan_patch_files() {
	local -r bbl_env_suffix="${1}"
	cp -r "${CI_DIR}/appbroker-bbl-plan-patch/." $(get_bbl_state_dir "${bbl_env_suffix}")
}

interpolate_terraform_template_file() {
	local -r internal_cidr="${1}" bbl_env_suffix="${2}"
	sed -e "s|<%= directorIp %>|$(calculate_director_ip ${internal_cidr})|;" "$(get_bbl_state_dir "${bbl_env_suffix}")/terraform/bosh-lite_override.tf.tmpl" > "$(get_bbl_state_dir "${bbl_env_suffix}")/terraform/bosh-lite_override.tf"
	sed -e "s|<%= internalCidr %>|${internal_cidr}|;" "$(get_bbl_state_dir "${bbl_env_suffix}")/terraform/bbl-template.tf.tmpl" > "$(get_bbl_state_dir "${bbl_env_suffix}")/terraform/bbl-template.tf"
}

create_director_and_jumpbox(){
	local -r internal_cidr="${1}"  bbl_env_suffix="${2}"
	DIRECTOR_INTERNAL_IP=$(calculate_director_ip "${internal_cidr}") \
	JUMPBOX_INTERNAL_IP=$(calculate_jumpbox_ip "${internal_cidr}") \
	INTERNAL_GATEWAY=$(calculate_gateway_ip "${internal_cidr}") \
	INTERNAL_CIDR="${internal_cidr}" \
	bbl up -s $(get_bbl_state_dir "${bbl_env_suffix}") --gcp-service-account-key="${CI_DIR}/gcp-key.json"
}

recreate_director_and_jumpbox(){
	local -r internal_cidr="${1}"  bbl_env_suffix="${2}"
	DIRECTOR_INTERNAL_IP=$(calculate_director_ip "${internal_cidr}") \
	JUMPBOX_INTERNAL_IP=$(calculate_jumpbox_ip "${internal_cidr}") \
	INTERNAL_GATEWAY=$(calculate_gateway_ip "${internal_cidr}") \
	INTERNAL_CIDR="${internal_cidr}" \
	RECREATE_DIRECTOR=true \
	bbl up -s $(get_bbl_state_dir "${bbl_env_suffix}") --gcp-service-account-key="${CI_DIR}/gcp-key.json"
}

source_bbl_environment() {
	local -r bbl_env_suffix="${1}"
	bbl print-env -s $(get_bbl_state_dir "${bbl_env_suffix}") --gcp-service-account-key="${CI_DIR}/gcp-key.json"> bbl-env.sh && source bbl-env.sh && rm bbl-env.sh
}

clone_cf_deployment(){
	local -r bbl_env_suffix="${1}"
	git clone git@github.com:cloudfoundry/cf-deployment.git "$(get_bbl_state_dir "${bbl_env_suffix}")/cf-deployment" || true
}

deploy_cf(){
	local -r bbl_env_suffix="${1}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	bosh -n update-runtime-config "${bbl_state_dir}/bosh-deployment/runtime-configs/dns.yml" --name dns
	local -r stemcell_version=$(bosh interpolate "${bbl_state_dir}/cf-deployment/cf-deployment.yml" --path /stemcells/alias=default/version)
	bosh -n upload-stemcell https://bosh.io/d/stemcells/bosh-warden-boshlite-ubuntu-trusty-go_agent?v=${stemcell_version}
	bosh -n -d cf deploy "${bbl_state_dir}/cf-deployment/cf-deployment.yml" -o "${bbl_state_dir}/cf-deployment/operations/bosh-lite.yml" \
		-o "${bbl_state_dir}/cf-deployment/operations/use-compiled-releases.yml" -v system_domain="$(get_bbl_env_name ${bbl_env_suffix}).cf-app.com"
}

update_dns(){
	local -r bbl_env_suffix="${1}"
	local -r director_external_ip=$(awk '/external_ip:/ {print $2}' < "$(get_bbl_state_dir "${bbl_env_suffix}")/vars/director-vars-file.yml")
	"${SCRIPT_DIR}/update-route53.sh" $(get_bbl_env_name  "${bbl_env_suffix}") "${director_external_ip}"
}

base64_encode_bbl_state(){

	local -r bbl_env_suffix="${1}" internal_cidr="${2}" upload_env_vars_to_circle="${3}"
	local -r bbl_env_suffix_upper="${bbl_env_suffix^^}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")

	declare -Ar bbl_state_file_paths=(
        ["BBL_${bbl_env_suffix_upper}_BBL_STATE_JSON"]="${bbl_state_dir}/bbl-state.json"
        ["BBL_${bbl_env_suffix_upper}_BBL_TFVARS"]="${bbl_state_dir}/vars/bbl.tfvars"
        ["BBL_${bbl_env_suffix_upper}_BOSH_STATE_JSON"]="${bbl_state_dir}/vars/bosh-state.json"
        ["BBL_${bbl_env_suffix_upper}_CLOUD_CONFIG_VARS_YML"]="${bbl_state_dir}/vars/cloud-config-vars.yml"
        ["BBL_${bbl_env_suffix_upper}_DIRECTOR_VARS_FILE_YML"]="${bbl_state_dir}/vars/director-vars-file.yml"
        ["BBL_${bbl_env_suffix_upper}_DIRECTOR_VARS_STORE_YML"]="${bbl_state_dir}/vars/director-vars-store.yml"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_STATE_JSON"]="${bbl_state_dir}/vars/jumpbox-state.json"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_VARS_FILE_YML"]="${bbl_state_dir}/vars/jumpbox-vars-file.yml"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_VARS_STORE_YML"]="${bbl_state_dir}/vars/jumpbox-vars-store.yml"
        ["BBL_${bbl_env_suffix_upper}_TERRAFORM_TFSTATE"]="${bbl_state_dir}/vars/terraform.tfstate"
    )
	[[ -f "${CI_DIR}/.envrc-localci" ]] || (printf "Ensure .envrc-localci is present in directory ${CI_DIR} before running script"; exit 1)
    update_internal_network_environment_variable "${bbl_env_suffix}" "${upload_env_vars_to_circle}" "${internal_cidr}"

    for var_name in "${!bbl_state_file_paths[@]}"; do
    	local base64_file_contents=$(base64 -w0 "${bbl_state_file_paths[${var_name}]}")
		if [[ ! -z "${base64_file_contents}" ]]; then
		#Delete previous variable in .envrc file then replace with new value
			sed -i -e "/${var_name}=/d" "${CI_DIR}/.envrc-localci"

			#Create a .envrc also in the bbl-state directory so the environment can be easily targeted manually
			#.envrc-localci is used by the local circleci runner
			printf "${var_name}=\"${base64_file_contents}\"\n" >> "${CI_DIR}/.envrc-localci"
			if [ "${upload_env_vars_to_circle}" = true ]; then
				upload_environment_variables_to_circleci "${var_name}" "${base64_file_contents}"
			fi
		else
			echo "${var_name} was empty, unable to decode variables"
			exit 1
    	fi
    done
	lastpass_upload_instructions
}

base64_decode_bbl_state(){
	local -r bbl_env_suffix="${1}"
	local -r bbl_env_suffix_upper="${bbl_env_suffix^^}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	declare -Ar bbl_state_file_paths=(
        ["BBL_${bbl_env_suffix_upper}_BBL_STATE_JSON"]="${bbl_state_dir}/bbl-state.json"
        ["BBL_${bbl_env_suffix_upper}_BBL_TFVARS"]="${bbl_state_dir}/vars/bbl.tfvars"
        ["BBL_${bbl_env_suffix_upper}_BOSH_STATE_JSON"]="${bbl_state_dir}/vars/bosh-state.json"
        ["BBL_${bbl_env_suffix_upper}_CLOUD_CONFIG_VARS_YML"]="${bbl_state_dir}/vars/cloud-config-vars.yml"
        ["BBL_${bbl_env_suffix_upper}_DIRECTOR_VARS_FILE_YML"]="${bbl_state_dir}/vars/director-vars-file.yml"
        ["BBL_${bbl_env_suffix_upper}_DIRECTOR_VARS_STORE_YML"]="${bbl_state_dir}/vars/director-vars-store.yml"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_STATE_JSON"]="${bbl_state_dir}/vars/jumpbox-state.json"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_VARS_FILE_YML"]="${bbl_state_dir}/vars/jumpbox-vars-file.yml"
        ["BBL_${bbl_env_suffix_upper}_JUMPBOX_VARS_STORE_YML"]="${bbl_state_dir}/vars/jumpbox-vars-store.yml"
        ["BBL_${bbl_env_suffix_upper}_TERRAFORM_TFSTATE"]="${bbl_state_dir}/vars/terraform.tfstate"
    )
    mkdir -p "${bbl_state_dir}/vars/"
	for var_name in "${!bbl_state_file_paths[@]}"; do
    	local decoded_file_contents=$(eval echo "\$${var_name}" | base64 -d)
    	if [[ ! -z "${decoded_file_contents}" ]]; then
        	echo "${decoded_file_contents}" > "${bbl_state_file_paths[${var_name}]}"
		else
			echo "${var_name} was empty, unable to decode variables"
			exit 1
		fi
    done
}

lastpass_upload_instructions(){
	cp "${CI_DIR}/.envrc-localci" "${CI_DIR}/envrc-local.txt"
	echo "LastPass cli can only upload attachments via the GUI\n"
	echo "Go to LastPass GUI and edit note: \"Shared-Spring Cloud Services /App Broker *LOCAL CI* .envrc-local file\""
	echo "Delete the previous attachment \"envrc-local.txt\""
	echo "then upload the modified env file ${CI_DIR}/envrc-local.txt as an attachment to the LastPass note"
}

upload_environment_variables_to_circleci(){
	local envvar_name="${1}"
	local envvar_value="${2}"

	curl -XPOST "${CIRCLE_API_BASE}/envvar?circle-token=${CIRCLECI_API_KEY}" \
		-H "Content-Type: application/json" -d '{"name": "'"${envvar_name}"'", "value": "'"${envvar_value}"'"}'
}

update_internal_network_environment_variable(){
	local -r bbl_env_suffix="${1}" upload_env_vars_to_circle="${2}" internal_cidr="${3}"
	local -r bbl_env_suffix_upper="${bbl_env_suffix^^}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	local -r internal_network_var_name="BBL_${bbl_env_suffix^^}_INTERNAL_NETWORK_CIDR"

	sed -i -e "/${internal_network_var_name}=/d" "${CI_DIR}/.envrc-localci"
	printf "${internal_network_var_name}=${internal_cidr}\n" >> "${CI_DIR}/.envrc-localci"
	if [ "${upload_env_vars_to_circle}" = true ]; then
		upload_environment_variables_to_circleci "${internal_network_var_name}" "${internal_cidr}"
	fi
}

update_variables_to_target_active_environment(){
	local -r bbl_env_suffix="${1}" ci_target="${2}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	local -r api_host_var_name="SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_API_HOST"
	local -r cf_password_var_name="SPRING_CLOUD_APPBROKER_ACCEPTANCETEST_CLOUDFOUNDRY_PASSWORD"
	local -r envrc_file="${CI_DIR}/.envrc-localci"

	source_bbl_environment "${bbl_env_suffix}"
	local -r bbl_env_id=$(bbl -s "${bbl_state_dir}" env-id)
	local -r cf_password=$(credhub get -n "/bosh-${bbl_env_id}/cf/cf_admin_password" -j | jq -r '.value')
	local -r api_host="api.appbroker-${bbl_env_suffix}.cf-app.com"
	if [[ -z "${cf_password}" ]]; then
		echo "Error: unable to find cf password"
		exit 1
	fi
	if [[ "$ci_target" != "local" && "$ci_target" != "circleci" ]]; then
		printf "-t option must equal local or circleci" 1>&2
		exit 1
	fi
	if [ "$ci_target" = "circleci" ]; then
		upload_environment_variables_to_circleci "${cf_password_var_name}" "${cf_password}"
		upload_environment_variables_to_circleci "${api_host_var_name}" "${api_host}"
		upload_environment_variables_to_circleci "ACTIVE_BBL_ENV_CIRCLE" "${bbl_env_suffix}"
		sed -i -e "/export ACTIVE_BBL_ENV_CIRCLE=/d" "${envrc_file}"
		printf "export ACTIVE_BBL_ENV_CIRCLE=${bbl_env_suffix}\\n" >> "${envrc_file}"

	fi
	if [ "$ci_target" = "local" ]; then
		sed -i -e "/export ACTIVE_BBL_ENV_LOCAL=/d" "${envrc_file}"
		printf "export ACTIVE_BBL_ENV_LOCAL=${bbl_env_suffix}\\n" >> "${envrc_file}"
	fi

	#Remove existing values from .envrc
	sed -i -e "/export ${api_host_var_name}=/d" "${envrc_file}"
	sed -i -e "/export ${cf_password_var_name}=/d" "${envrc_file}"

	printf "export ${api_host_var_name}=${api_host}\\n" >> "${envrc_file}"
	printf "export ${cf_password_var_name}=${cf_password}\\n" >> "${envrc_file}"
	lastpass_upload_instructions
}

recreate_environment(){
	local -r bbl_env_suffix="${1}"
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")
	local -r bbl_suffix_upper=${bbl_env_suffix^^}
	local -r internal_network_var_name="BBL_${bbl_suffix_upper}_INTERNAL_NETWORK_CIDR"
	local -r internal_cidr=$(eval echo "\$${internal_network_var_name}")
	source_bbl_environment "${bbl_env_suffix}"
	clone_cf_deployment "${bbl_env_suffix}"

	bosh -n update-runtime-config "${bbl_state_dir}/bosh-deployment/runtime-configs/dns.yml" --name dns
	local -r stemcell_version=$(bosh interpolate "${bbl_state_dir}/cf-deployment/cf-deployment.yml" --path /stemcells/alias=default/version)
	bosh -n upload-stemcell https://bosh.io/d/stemcells/bosh-warden-boshlite-ubuntu-xenial-go_agent?v=${stemcell_version}
	local -r cf_password=$(credhub get -n "/bosh-${bbl_env_suffix}/cf/cf_admin_password" -j | jq -r '.value')
	bosh -n -d cf delete-deployment
	bosh -n -d cf deploy "${bbl_state_dir}/cf-deployment/cf-deployment.yml" -o "${bbl_state_dir}/cf-deployment/operations/bosh-lite.yml" \
		-o "${bbl_state_dir}/cf-deployment/operations/use-compiled-releases.yml" -v system_domain="$(get_bbl_env_name ${bbl_env_suffix}).cf-app.com" -v cf_admin_password="${cf_password}"
}

get_bbl_state_dir(){
	local -r bbl_env_suffix="${1}"
	echo "${CI_DIR}/bosh-env-${bbl_env_suffix}/bbl-state"
}

get_bbl_env_name(){
	local -r bbl_env_suffix="${1}"
	echo "appbroker-${bbl_env_suffix}"
}

generate_bbl_state_dir_for_environment(){
	local -r bbl_env_suffix="${1}"
	local -r bbl_suffix_upper=${bbl_env_suffix^^}
	local -r bbl_state_dir=$(get_bbl_state_dir "${bbl_env_suffix}")

	if [[ -z "${CIRCLE_BUILD_NUM:-}" ]] ; then
		source_environment_variables
		[[ -f "${CI_DIR}/.envrc-localci" ]] || (printf "Ensure .envrc is present in directory ${CI_DIR} before running script"; exit 1)
		source "${CI_DIR}/.envrc-localci"
	fi
	local -r internal_network_var_name="BBL_${bbl_suffix_upper}_INTERNAL_NETWORK_CIDR"
	local -r internal_cidr=$(eval echo "\$${internal_network_var_name}")
	create_bbl_state_dir "${bbl_env_suffix}"
	#Just for creating files required by bbl, ignore any failures
	bbl plan --name "${bbl_env_suffix}" -s "${bbl_state_dir}" --gcp-service-account-key="${CI_DIR}/gcp-key.json" || true
	copy_plan_patch_files "${bbl_env_suffix}"
	interpolate_terraform_template_file "${internal_cidr}" "${bbl_env_suffix}"
	base64_decode_bbl_state "${bbl_env_suffix}"
	echo "Finished creating generating BBL state directory for $(get_bbl_env_name ${bbl_env_suffix})"
}

create_new_bbl_bosh_environment(){
	local -r bbl_env_suffix="${1}" internal_network_cidr="${2}" upload_env_vars_to_circleci="${3}"
	validate "${internal_network_cidr}"
	create_bbl_state_dir "${bbl_env_suffix}"
	create_new_bbl_plan "${bbl_env_suffix}"
	copy_plan_patch_files "${bbl_env_suffix}"
	interpolate_terraform_template_file "${internal_network_cidr}" "${bbl_env_suffix}"
	create_director_and_jumpbox "${internal_network_cidr}" "${bbl_env_suffix}"
	source_bbl_environment "${bbl_env_suffix}"
	clone_cf_deployment "${bbl_env_suffix}"
	deploy_cf "${bbl_env_suffix}"
	update_dns "${bbl_env_suffix}"
	base64_encode_bbl_state	"${bbl_env_suffix}" "${internal_network_cidr}" "${upload_env_vars_to_circleci}"
	echo "Finished creating cloud foundry environment, you can now target CF using 'cf api api.$(get_bbl_env_name ${bbl_env_suffix}).cf-app.com --skip-ssl-validation'"
}

process_command(){
	local -r arg="${1}"
	local OPTIND
	local upload_env_vars_to_circleci=false
	local upload_env_vars_to_lastpass=false
	local internal_network_cidr
	local bbl_env_suffix
	local ci_target

	case "${arg}" in
		create-new-environment)
			shift
			while getopts ":ei:b:" FOUND "${@}";
			do
				case ${FOUND} in
					e) upload_env_vars_to_circleci=true
						;;
					i) internal_network_cidr="${OPTARG}"
						;;
					b) bbl_env_suffix="${OPTARG}"
						;;
					\:) printf "argument missing from -%s option\n" $OPTARG
						usage
						exit 2
						;;
					\?) printf "unknown option: -%s\n" $OPTARG
						usage
						exit 2
						;;
				esac >&2
			done
			shift $(($OPTIND - 1))
			: ${bbl_env_suffix:?"Required Env Variable not found!"}
			: ${internal_network_cidr:?"Required Env Variable not found!"}
			create_new_bbl_bosh_environment "${bbl_env_suffix}" "${internal_network_cidr}" "${upload_env_vars_to_circleci}"
			;;

		generate-bbl-state-directory)
			shift
			while getopts ":b:" FOUND "${@}";
			do
				case ${FOUND} in
					b) bbl_env_suffix="${OPTARG}"
						;;
					\:) printf "argument missing from -%s option\n" $OPTARG
						usage
						exit 2
						;;
					\?) printf "unknown option: -%s\n" $OPTARG
						usage
						exit 2
						;;
				esac >&2
			done
			shift $(($OPTIND - 1))
			: ${bbl_env_suffix:?"Required Env Variable not found!"}
			generate_bbl_state_dir_for_environment "${bbl_env_suffix}"
			;;

		set-active-environment)
			shift
			while getopts ":b:t:" FOUND "${@}";
			do
				case ${FOUND} in
					b) bbl_env_suffix="${OPTARG}"
						;;
					t) ci_target="${OPTARG}"
						;;
					\:) printf "argument missing from -%s option\n" $OPTARG
						usage
						exit 2
						;;
					\?) printf "unknown option: -%s\n" $OPTARG
						usage
						exit 2
						;;
				esac >&2
			done
			generate_bbl_state_dir_for_environment "${bbl_env_suffix}"
			update_variables_to_target_active_environment "${bbl_env_suffix}" "${ci_target}"
			;;

		recreate-environment)
			shift
			while getopts ":b:" FOUND "${@}";
			do
				case ${FOUND} in
					b) bbl_env_suffix="${OPTARG}"
						;;
					\:) printf "argument missing from -%s option\n" $OPTARG
						usage
						exit 2
						;;
					\?) printf "unknown option: -%s\n" $OPTARG
						usage
						exit 2
						;;
				esac >&2
			done
			generate_bbl_state_dir_for_environment "${bbl_env_suffix}"
			recreate_environment "${bbl_env_suffix}"
			;;

		delete-environment)
			printf "Command not yet implemented"
			;;
		*)
			printf "${arg}: Unknown command"
			;;
    esac
}

[[ $# -lt 1 ]] && printf "Not enough arguments\n\n" && usage && exit 1
process_command "${@}"
