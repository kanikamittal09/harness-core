#!/usr/bin/env bash
set -e

INSTALLER_DIR=harness_installer
INSTALLER_COMPRESSED_FILE=harness_installer.tar.gz
INSTALLER_TEMPLATE_DIR=harness_disconnected_on_prem_pov_final
SCRIPTS_DIR=scripts
CONFIG_PROPERTIES_FILE="${INSTALLER_DIR}/config.properties"
FIRST_TIME_INSTALL_SCRIPT_FILE=first_time_only_install_harness.sh
UPGRADE_SCRIPT_FILE=upgrade_harness.sh
VERSION_PROPERTIES_FILE=version.properties

IMAGES_DIR="${INSTALLER_DIR}/images"
MANAGER_IMAGE="harness/manager:${MANAGER_VERSION}"
VERIFICATION_SERVICE_IMAGE="harness/verification-service:${VERIFICATION_SERVICE_VERSION}"
LEARNING_ENGINE_IMAGE="harness/learning-engine-onprem:${LEARNING_ENGINE_VERSION}"
UI_IMAGE="harness/ui:${UI_VERSION}"
PROXY_IMAGE="harness/proxy:${PROXY_VERSION}"
MONGO_IMAGE="mongo:${MONGO_VERSION}"

MANAGER_IMAGE_TAR="${IMAGES_DIR}/manager.tar"
VERIFICATION_SERVICE_IMAGE_TAR="${IMAGES_DIR}/verification_service.tar"
LEARNING_ENGINE_IMAGE_TAR="${IMAGES_DIR}/learning_engine.tar"
UI_IMAGE_TAR="${IMAGES_DIR}/ui.tar"
PROXY_IMAGE_TAR="${IMAGES_DIR}/proxy.tar"
MONGO_IMAGE_TAR="${IMAGES_DIR}/mongo.tar"

JRE_SOURCE_URL_1=https://app.harness.io/storage/wingsdelegates/jre/8u131
JRE_SOLARIS_1=jre-8u131-solaris-x64.tar.gz
JRE_MACOSX_1=jre-8u131-macosx-x64.tar.gz
JRE_LINUX_1=jre-8u131-linux-x64.tar.gz

JRE_SOURCE_URL_2=https://app.harness.io/storage/wingsdelegates/jre/8u191
JRE_SOLARIS_2=jre-8u191-solaris-x64.tar.gz
JRE_MACOSX_2=jre-8u191-macosx-x64.tar.gz
JRE_LINUX_2=jre-8u191-linux-x64.tar.gz

KUBECTL_VERSION=v1.13.2
GOTEMPLATE_VERSION=v0.2

KUBECTL_LINUX_DIR="${IMAGES_DIR}/kubectl/linux/$KUBECTL_VERSION/"
KUBECTL_MAC_DIR="${IMAGES_DIR}/kubectl/darwin/$KUBECTL_VERSION/"

GOTEMPLATE_LINUX_DIR="${IMAGES_DIR}/go-template/linux/$GOTEMPLATE_VERSION/"
GOTEMPLATE_MAC_DIR="${IMAGES_DIR}/go-template/darwin/$GOTEMPLATE_VERSION/"

echo "$KUBECTL_MAC_DIR"
echo "$KUBECTL_LINUX_DIR"
echo "$GOTEMPLATE_MAC_DIR"
echo "$GOTEMPLATE_LINUX_DIR"


KUBECTL_LINUX_URL=https://app.harness.io/storage/harness-download/kubernetes-release/release/"$KUBECTL_VERSION"/bin/linux/amd64/kubectl
KUBECTL_MAC_URL=https://app.harness.io/storage/harness-download/kubernetes-release/release/"$KUBECTL_VERSION"/bin/darwin/amd64/kubectl

GOTEMPLATE_LINUX_URL=https://app.harness.io/storage/harness-download/snapshot-go-template/release/"$GOTEMPLATE_VERSION"/bin/linux/amd64/go-template
GOTEMPLATE_MAC_URL=https://app.harness.io/storage/harness-download/snapshot-go-template/release/"$GOTEMPLATE_VERSION"/bin/darwin/amd64/go-template

rm -f "${INSTALLER_COMPRESSED_FILE}"

rm -rf "${INSTALLER_DIR}"
mkdir -p "${INSTALLER_DIR}"
mkdir -p "${IMAGES_DIR}"
cp README.txt "${INSTALLER_DIR}"

echo "Manager version is ${MANAGER_VERSION}"
echo "Mongo version is ${MONGO_VERSION}"
echo "Verification Service version is ${VERIFICATION_SERVICE_VERSION}"
echo "Delegate version is ${DELEGATE_VERSION}"
echo "Watcher version is ${WATCHER_VERSION}"
echo "Proxy version is ${PROXY_VERSION}"
echo "UI version is ${UI_VERSION}"
echo "Learning Engine version is ${LEARNING_ENGINE_VERSION}"
echo "kubectl version is ${KUBECTL_VERSION}"
echo "go-template version is ${GOTEMPLATE_VERSION}"

cp -r ../${INSTALLER_TEMPLATE_DIR}/* ${INSTALLER_DIR}/
cp "${VERSION_PROPERTIES_FILE}" "${INSTALLER_DIR}/"

mkdir -p $KUBECTL_LINUX_DIR
mkdir -p $KUBECTL_MAC_DIR
mkdir -p $GOTEMPLATE_LINUX_DIR
mkdir -p $GOTEMPLATE_MAC_DIR

if [[ -z $1 ]]; then
   echo "No license file supplied, skipping setting the license file in the installer"
else
   echo "License file supplied, generating installer with license file $1"
   sed -i "s|harness_license|$1|g" "${CONFIG_PROPERTIES_FILE}"
fi

docker login -u ${DOCKERHUB_USERNAME} -p ${DOCKERHUB_PASSWORD}
docker pull "${MANAGER_IMAGE}"
docker pull "${VERIFICATION_SERVICE_IMAGE}"
docker pull "${LEARNING_ENGINE_IMAGE}"
docker pull "${UI_IMAGE}"
docker pull "${PROXY_IMAGE}"
docker pull "${MONGO_IMAGE}"

docker save "${MANAGER_IMAGE}" > "${MANAGER_IMAGE_TAR}"
docker save "${VERIFICATION_SERVICE_IMAGE}" > "${VERIFICATION_SERVICE_IMAGE_TAR}"
docker save "${LEARNING_ENGINE_IMAGE}" > "${LEARNING_ENGINE_IMAGE_TAR}"
docker save "${UI_IMAGE}" > "${UI_IMAGE_TAR}"
docker save "${PROXY_IMAGE}" > "${PROXY_IMAGE_TAR}"
docker save "${MONGO_IMAGE}" > "${MONGO_IMAGE_TAR}"

curl "${JRE_SOURCE_URL_1}/${JRE_SOLARIS_1}" > "${JRE_SOLARIS_1}"
curl "${JRE_SOURCE_URL_1}/${JRE_MACOSX_1}" > "${JRE_MACOSX_1}"
curl "${JRE_SOURCE_URL_1}/${JRE_LINUX_1}" > "${JRE_LINUX_1}"

curl "${JRE_SOURCE_URL_2}/${JRE_SOLARIS_2}" > "${JRE_SOLARIS_2}"
curl "${JRE_SOURCE_URL_2}/${JRE_MACOSX_2}" > "${JRE_MACOSX_2}"
curl "${JRE_SOURCE_URL_2}/${JRE_LINUX_2}" > "${JRE_LINUX_2}"

curl -L -o "${KUBECTL_MAC_DIR}kubectl" "${KUBECTL_MAC_URL}"
curl -L -o "${KUBECTL_LINUX_DIR}kubectl" "${KUBECTL_LINUX_URL}"

curl -L -o "${GOTEMPLATE_LINUX_DIR}go-template" "${GOTEMPLATE_LINUX_URL}"
curl -L -o "${GOTEMPLATE_MAC_DIR}go-template" "${GOTEMPLATE_MAC_URL}"


cp delegate.jar "${IMAGES_DIR}/"
cp watcher.jar "${IMAGES_DIR}/"
mv "${JRE_SOLARIS_1}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX_1}" "${IMAGES_DIR}/"
mv "${JRE_LINUX_1}" "${IMAGES_DIR}/"

mv "${JRE_SOLARIS_2}" "${IMAGES_DIR}/"
mv "${JRE_MACOSX_2}" "${IMAGES_DIR}/"
mv "${JRE_LINUX_2}" "${IMAGES_DIR}/"

tar -cvzf "${INSTALLER_COMPRESSED_FILE}" "${INSTALLER_DIR}"
#rm -rf "${INSTALLER_DIR}"