FROM rundeck/ubuntu-base

# The war is compiled to Java 25 bytecode (class file 69), so the runtime has to match.
# rundeck/ubuntu-base:latest on Docker Hub is Ubuntu 20.04, whose archive stops at openjdk-21
# (the local docker/ubuntu-base/Dockerfile is 22.04, but that image is only rebuilt by the docker
# build job, not by this one). Adoptium publishes Temurin 25 for focal, so take it from there.
USER root
RUN apt-get update && \
    apt-get install -y wget gnupg && \
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor > /usr/share/keyrings/adoptium.gpg && \
    echo "deb [signed-by=/usr/share/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb focal main" > /etc/apt/sources.list.d/adoptium.list && \
    apt-get update && \
    apt-get install -y temurin-25-jre && \
    ln -s /usr/lib/jvm/temurin-25-jre-* /usr/lib/jvm/java-25 && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Symlinked above because Temurin's own path carries the architecture (-amd64 / -arm64).
ENV JAVA_HOME=/usr/lib/jvm/java-25
ENV PATH=$JAVA_HOME/bin:$PATH

USER rundeck
COPY --chown=rundeck:root scripts/rd-util.sh /rd-util.sh
ADD --chown=rundeck:root scripts/deb-tests.sh /init-tests.sh
