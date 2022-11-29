FROM harbor-repo.vmware.com/dockerhub-proxy-cache/library/ubuntu:bionic

RUN apt-get update && apt-get install --no-install-recommends -y \
    ca-certificates \
    curl \
    git \
    gnupg \
    jq \
    net-tools

RUN mkdir -p /etc/apt/keyrings
RUN curl -L 'https://cli.github.com/packages/githubcli-archive-keyring.gpg' | gpg --dearmor -o /etc/apt/keyrings/githubcli-archive-keyring.gpg
RUN curl -L 'https://packages.cloudfoundry.org/debian/cli.cloudfoundry.org.key' | gpg --dearmor -o /etc/apt/keyrings/cloudfoundry-cli-keyring.gpg

RUN echo "deb [signed-by=/etc/apt/keyrings/cloudfoundry-cli-keyring.gpg] https://packages.cloudfoundry.org/debian stable main" | tee /etc/apt/sources.list.d/cloudfoundry-cli.list
RUN echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | tee /etc/apt/sources.list.d/github-cli.list

RUN apt-get update &&  \
    apt-get -qy --no-install-recommends install \
		cf-cli \
		gh && \
    apt-get clean

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
RUN mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u292-b10/OpenJDK8U-jdk_x64_linux_hotspot_8u292b10.tar.gz | tar xz --strip-components=1

ADD https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.4/concourse-java.sh /opt/
ADD https://repo.spring.io/libs-snapshot/io/spring/concourse/releasescripts/concourse-release-scripts/0.3.4-SNAPSHOT/concourse-release-scripts-0.3.4-SNAPSHOT.jar /opt/

RUN cd /usr/local/bin && curl -L https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/2.9.0/credhub-linux-2.9.0.tgz | tar xz

RUN curl -L https://github.com/cloudfoundry/bosh-bootloader/releases/download/v8.4.43/bbl-v8.4.43_linux_x86-64 --output /usr/local/bin/bbl && \
    chmod +x /usr/local/bin/bbl

RUN curl -L https://github.com/cloudfoundry/bosh-cli/releases/download/v6.4.4/bosh-cli-6.4.4-linux-amd64 --output /usr/local/bin/bosh && \
    chmod +x /usr/local/bin/bosh
