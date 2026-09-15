FROM rundeck/ubuntu-base

# The war is compiled to Java 25 bytecode (class file 69), so the runtime has to match.
# rundeck/ubuntu-base is ubuntu:22.04, which carries openjdk-25 (25.0.4+7-1~22.04).
USER root
RUN apt-get update && \
    apt-get install -y openjdk-25-jre-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

USER rundeck
COPY --chown=rundeck:root scripts/rd-util.sh /rd-util.sh
ADD --chown=rundeck:root scripts/deb-tests.sh /init-tests.sh
