FROM circleci/openjdk:8-jdk

RUN BOSH_URL="https://s3.amazonaws.com/bosh-cli-artifacts/bosh-cli-5.0.1-linux-amd64"&& \
  sudo curl --silent --show-error --location --fail --retry 3 --output /usr/bin/bosh $BOSH_URL && \
  sudo chmod +x /usr/bin/bosh && \
  bosh -v

RUN BBL_URL="https://github.com/cloudfoundry/bosh-bootloader/releases/download/v6.8.4/bbl-v6.8.4_linux_x86-64" && \
  sudo curl --silent --show-error --location --fail --retry 3 --output /usr/bin/bbl $BBL_URL && \
  sudo chmod +x /usr/bin/bbl && \
  bbl -v

RUN sudo apt-get install python3