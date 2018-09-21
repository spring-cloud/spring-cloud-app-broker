FROM circleci/openjdk:8-jdk

RUN BOSH_URL="https://s3.amazonaws.com/bosh-cli-artifacts/bosh-cli-5.0.1-linux-amd64"&& \
  sudo curl --silent --show-error --location --fail --retry 3 --output /usr/bin/bosh $BOSH_URL && \
  sudo chmod +x /usr/bin/bosh && \
  bosh -v

RUN BBL_URL="https://github.com/cloudfoundry/bosh-bootloader/releases/download/v6.8.4/bbl-v6.8.4_linux_x86-64" && \
  sudo curl --silent --show-error --location --fail --retry 3 --output /usr/bin/bbl $BBL_URL && \
  sudo chmod +x /usr/bin/bbl && \
  bbl -v

RUN sudo apt-get -qqy install python3 \
	lsb-release \
	apt-transport-https

RUN export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)" && \
    echo "deb https://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee /etc/apt/sources.list.d/google-cloud-sdk.list && \
    sudo curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add - && \
    sudo apt-get update && \
    sudo apt-get install -y google-cloud-sdk && \
    gcloud config set core/disable_usage_reporting true && \
    gcloud config set component_manager/disable_update_check true && \
    gcloud --version