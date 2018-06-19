FROM rundeckapp/testdeck:base-latest

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

ARG CLI_DEB_URL
ARG CLI_VERS

    # Add gpg key
RUN curl "https://bintray.com/user/downloadSubjectPublicKey?username=bintray" > /tmp/bintray.gpg.key \
    && apt-key add - < /tmp/bintray.gpg.key \
    # Add cli tool debian repo
    && echo "deb $CLI_DEB_URL /" | sudo tee -a /etc/apt/sources.list \
    # RUNDECK - create user
    && adduser --shell /bin/bash --home $HOME --gecos "" --disabled-password $USERNAME && \
        passwd -d $USERNAME && \
        addgroup $USERNAME sudo

WORKDIR $HOME

COPY --chown=rundeck:rundeck data $HOME
COPY --chown=rundeck:rundeck api_test $HOME/api_test

ARG LAUNCHER_URL
ARG RUNDECK_NODE=rundeck1
RUN echo "download rundeck launcher: ${LAUNCHER_URL}" \
    && { test -f $HOME/rundeck-launcher.war || curl -sS -f -L ${LAUNCHER_URL} > $HOME/rundeck-launcher.war; } \
    && { test -f $HOME/data/rd.deb && dpkg -i $HOME/data/rd.deb || true; } \
    && { test -f $HOME/data/rd.deb || apt-get -y update && apt-get -y install rundeck-cli=$CLI_VERS; } \
    && java \
        -Dserver.http.port=4440 \
        -Dserver.hostname=$RUNDECK_NODE \
        -jar $HOME/rundeck-launcher.war --installonly \
    && rm -rf /var/lib/apt/lists/*

USER rundeck


# Copy files.
RUN mkdir -p $HOME/scripts
COPY scripts $HOME/scripts
RUN sudo chmod -R a+x $HOME/scripts/*

RUN sudo mkdir -p /tests
COPY tests /tests
RUN sudo chmod -R a+x /tests/*

VOLUME /var/lib/docker
VOLUME /test
VOLUME $HOME/resources

EXPOSE 22 4440 4443 4444

# Start the instance.
CMD $HOME/scripts/run.sh