FROM rundeck/ubuntu-base@sha256:4cb03aa583f56c2cf739cae24c525de7d52d3e490a0c013fe498ca83e4bb799c

RUN set -euxo pipefail \
    # Install confd
    && wget https://github.com/kelseyhightower/confd/releases/download/v0.16.0/confd-0.16.0-linux-amd64 \
    && mv confd* /usr/local/bin/confd \
    && chmod ugo+x /usr/local/bin/confd \
    # Setup rundeck user
    && adduser --shell /bin/bash --home /home/rundeck --gecos "" --disabled-password rundeck \
    && passwd -d rundeck \
    && addgroup rundeck sudo \
    && echo | sudo -u rundeck ssh-keygen -N ''

USER rundeck

WORKDIR /home/rundeck

COPY --chown=rundeck:rundeck .build .
RUN java -jar rundeck.war --installonly

COPY --chown=rundeck:rundeck confd /etc/confd

VOLUME ["/home/rundeck/server/data"]

EXPOSE 4440
CMD confd -onetime -backend env \
    && \
    java \
        -XX:+UnlockExperimentalVMOptions \
        -XX:MaxRAMFraction=1 \
        -XX:+UseCGroupMemoryLimitForHeap \
        -Dloginmodule.conf.name=jaas-loginmodule.conf \
        -Dloginmodule.name=rundeck \
        -Drundeck.jaaslogin=true \
        -jar rundeck.war