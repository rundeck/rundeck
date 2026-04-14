FROM rundeck/ubuntu-base

# Grails 7: Install Java 17 for Rundeck runtime
# Note: ubuntu-base:latest on Docker Hub is currently Ubuntu 20.04.3
# TODO: Rebuild and push Ubuntu 22.04 image to Docker Hub
USER root
RUN apt-get update && \
    apt-get install -y openjdk-17-jre-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

USER rundeck
COPY --chown=rundeck:root scripts/rd-util.sh /rd-util.sh
ADD --chown=rundeck:root scripts/deb-tests.sh /init-tests.sh
