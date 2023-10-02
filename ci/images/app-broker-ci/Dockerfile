FROM harbor-repo.vmware.com/dockerhub-proxy-cache/library/ubuntu:bionic

ARG BBL_CLI_VERSION=8.4.110
ARG BOSH_CLI_VERSION=7.0.1
ARG CONCOURSE_JAVA_SCRIPTS_VERSION=0.0.4
ARG CONCOURSE_RELEASE_SCRIPTS_VERSION=0.3.4
ARG CREDHUB_CLI_VERSION=2.9.8

RUN apt-get update && apt-get install --no-install-recommends -y ca-certificates net-tools git curl jq gnupg

RUN curl -L https://packages.cloudfoundry.org/debian/cli.cloudfoundry.org.key | apt-key add -
RUN echo "deb https://packages.cloudfoundry.org/debian stable main" | tee /etc/apt/sources.list.d/cloudfoundry-cli.list
RUN apt-get update && apt-get install cf-cli

RUN rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
RUN mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u292-b10/OpenJDK8U-jdk_x64_linux_hotspot_8u292b10.tar.gz | tar xz --strip-components=1

ADD "https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v$CONCOURSE_JAVA_SCRIPTS_VERSION/concourse-java.sh" /opt/

RUN cd /usr/local/bin && curl -L "https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/$CREDHUB_CLI_VERSION/credhub-linux-$CREDHUB_CLI_VERSION.tgz" | tar xz

RUN curl -L "https://github.com/cloudfoundry/bosh-bootloader/releases/download/v$BBL_CLI_VERSION/bbl-v${BBL_CLI_VERSION}_linux_x86-64" --output /usr/local/bin/bbl && \
    chmod +x /usr/local/bin/bbl

RUN curl -L "https://github.com/cloudfoundry/bosh-cli/releases/download/v$BOSH_CLI_VERSION/bosh-cli-$BOSH_CLI_VERSION-linux-amd64" --output /usr/local/bin/bosh && \
    chmod +x /usr/local/bin/bosh
