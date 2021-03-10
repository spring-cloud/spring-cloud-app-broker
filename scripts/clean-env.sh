#!/usr/bin/env bash

TEST_ORG=test
TEST_INSTANCES_ORG=test-instances
TEST_SPACE=development
GENERATED_SPACE=$1

cf target -o ${TEST_ORG} -s ${TEST_SPACE}

cf purge-service-instance -f backing-service-instance-created
cf purge-service-instance -f backing-service-instance-existing
cf purge-service-instance -f backing-service-instance-update
cf purge-service-instance -f backing-service-instance-new
cf purge-service-instance -f backing-service-instance-old
cf purge-service-instance -f backing-si-cb193dd5-5f96-4337-8274-3aab71a71145
cf purge-service-instance -f backing-si-75490152-99e2-4d8e-a610-3450c9592633
cf purge-service-instance -f si-create
cf purge-service-instance -f si-create-create-si-guid
cf purge-service-instance -f si-create-oauth2
cf purge-service-instance -f si-create-params
cf purge-service-instance -f si-create-space-per
cf purge-service-instance -f si-create-services
cf purge-service-instance -f si-update-domain
cf purge-service-instance -f si-update-services
cf purge-service-instance -f si-update
cf purge-service-instance -f si-update-with-new-services
cf purge-service-instance -f si-binding

cf d -f app-update
cf d -f app-update-domain
cf d -f app-create-services
cf d -f app-update-services
cf d -f app-create-oauth2
cf d -f app-create-cr-862a81c0-18a8-4359-aa78-79c50a88cc00
cf d -f app-create-cr-75490152-99e2-4d8e-a610-3450c9592633
cf d -f app-create-2
cf d -f app-create-1
cf d -f app-create-params
cf d -f test-broker-app-update-with-new-services
cf d -f backing-app-binding
cf d -f bound-app
cf d -f app-update-with-new-services
cf d -f app-create-cr-cb193dd5-5f96-4337-8274-3aab71a71145

cf delete-orphaned-routes -f

cf target -o ${TEST_ORG} -s ${GENERATED_SPACE}
cf purge-service-instance -f si-create-services
cf purge-service-instance -f backing-service-space-per-target
cf d -f app-create-space-per1
cf delete-orphaned-routes -f
cf apps
cf services
cf routes

cf target -o ${TEST_ORG} -s ${TEST_SPACE}
cf delete-space -f ${GENERATED_SPACE}

cf spaces
cf apps
cf services
cf routes

cf target -o ${TEST_INSTANCES_ORG} -s ${TEST_SPACE}
cf purge-service-instance -f si-create
cf purge-service-instance -f si-create-create-si-guid
cf purge-service-instance -f si-create-oauth2
cf purge-service-instance -f si-create-params
cf purge-service-instance -f si-create-services
cf purge-service-instance -f si-create-space-per
cf purge-service-instance -f si-managedapp-management-restage
cf purge-service-instance -f si-managedapp-management-restart
cf purge-service-instance -f si-managedapp-management-start
cf purge-service-instance -f si-managedapp-management-stop
cf purge-service-instance -f si-update
cf purge-service-instance -f si-update-domain
cf purge-service-instance -f si-update-services
cf purge-service-instance -f si-update-target
cf purge-service-instance -f si-update-with-new-services
cf purge-service-instance -f si-upgrade

cf spaces
cf apps
cf services
cf routes

cf target -o system -s system

cf delete-org -f ${TEST_INSTANCES_ORG}
cf create-org ${TEST_INSTANCES_ORG} -q runaway
cf target -o ${TEST_INSTANCES_ORG}
cf create-space ${TEST_SPACE}

cf delete-org -f ${TEST_ORG}
cf create-org ${TEST_ORG} -q runaway
cf target -o ${TEST_ORG}
cf create-space ${TEST_SPACE}

cf target -o ${TEST_ORG} -s ${TEST_SPACE}

cf delete-service-broker test-broker-create-instance-space-per-si -f
cf delete-service-broker test-broker-create-instance-with-services -f
