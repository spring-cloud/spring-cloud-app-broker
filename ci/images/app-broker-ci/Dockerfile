FROM spring-app-broker-docker-virtual.usw1.packages.broadcom.com/ubuntu:jammy

ARG BBL_CLI_VERSION=8.4.110
ARG BOSH_CLI_VERSION=7.0.1
ARG CONCOURSE_JAVA_SCRIPTS_VERSION=0.0.4
ARG CONCOURSE_RELEASE_SCRIPTS_VERSION=0.3.4
ARG CREDHUB_CLI_VERSION=2.9.8

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    	ca-certificates \
    	curl \
    	gnupg && \
    apt-get clean

RUN mkdir -p /etc/apt/keyrings
RUN curl -L 'https://cli.github.com/packages/githubcli-archive-keyring.gpg' | gpg --dearmor -o /etc/apt/keyrings/githubcli-archive-keyring.gpg
RUN curl -L 'https://packages.cloudfoundry.org/debian/cli.cloudfoundry.org.key' | gpg --dearmor -o /etc/apt/keyrings/cloudfoundry-cli-keyring.gpg

RUN echo "deb [signed-by=/etc/apt/keyrings/cloudfoundry-cli-keyring.gpg] https://packages.cloudfoundry.org/debian stable main" | tee /etc/apt/sources.list.d/cloudfoundry-cli.list
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list

RUN apt-get update && \
    apt-get install --no-install-recommends -y \
    	cf8-cli \
    	gh \
    	git \
		jq \
		net-tools \
    	openjdk-17-jdk-headless && \
    apt-get clean

ADD "https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v$CONCOURSE_JAVA_SCRIPTS_VERSION/concourse-java.sh" /opt/

RUN cd /usr/local/bin && curl -L "https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/$CREDHUB_CLI_VERSION/credhub-linux-$CREDHUB_CLI_VERSION.tgz" | tar xz

RUN curl -L "https://github.com/cloudfoundry/bosh-bootloader/releases/download/v$BBL_CLI_VERSION/bbl-v${BBL_CLI_VERSION}_linux_x86-64" --output /usr/local/bin/bbl && \
    chmod +x /usr/local/bin/bbl

RUN curl -L "https://github.com/cloudfoundry/bosh-cli/releases/download/v$BOSH_CLI_VERSION/bosh-cli-$BOSH_CLI_VERSION-linux-amd64" --output /usr/local/bin/bosh && \
    chmod +x /usr/local/bin/bosh

RUN cd /usr/local/bin && curl -L 'https://packages.cloudfoundry.org/stable?release=linux64-binary&version=v7&source=github' | tar xz && \
    chmod +x /usr/local/bin/cf
