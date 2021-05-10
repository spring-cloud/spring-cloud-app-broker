FROM harbor-repo.vmware.com/dockerhub-proxy-cache/library/ubuntu:bionic

RUN apt-get update && apt-get install --no-install-recommends -y ca-certificates net-tools git curl jq gnupg

RUN curl -L https://packages.cloudfoundry.org/debian/cli.cloudfoundry.org.key | apt-key add -
RUN echo "deb https://packages.cloudfoundry.org/debian stable main" | tee /etc/apt/sources.list.d/cloudfoundry-cli.list
RUN apt-get update && apt-get install cf-cli

RUN rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME /opt/openjdk
ENV PATH $JAVA_HOME/bin:$PATH
RUN mkdir -p /opt/openjdk && \
    cd /opt/openjdk && \
    curl -L https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u192-b12/OpenJDK8U-jdk_x64_linux_hotspot_8u192b12.tar.gz | tar xz --strip-components=2

ADD https://raw.githubusercontent.com/spring-io/concourse-java-scripts/v0.0.3/concourse-java.sh /opt/
ADD https://repo.spring.io/libs-release/io/spring/concourse/releasescripts/concourse-release-scripts/0.3.2/concourse-release-scripts-0.3.2.jar /opt/

RUN cd /usr/local/bin && curl -L https://github.com/cloudfoundry-incubator/credhub-cli/releases/download/2.9.0/credhub-linux-2.9.0.tgz | tar xz

RUN curl -L https://github.com/cloudfoundry/bosh-bootloader/releases/download/v8.4.40/bbl-v8.4.40_linux_x86-64 --output /usr/local/bin/bbl && \
    chmod +x /usr/local/bin/bbl

RUN curl -L https://github.com/cloudfoundry/bosh-cli/releases/download/v6.4.3/bosh-cli-6.4.3-linux-amd64 --output /usr/local/bin/bosh && \
    chmod +x /usr/local/bin/bosh
