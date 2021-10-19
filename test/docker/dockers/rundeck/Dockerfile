FROM rundeckapp/testdeck:base-latest

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

ARG CLI_DEB_URL
ARG CLI_VERS

#Add PAM test user

RUN curl -s https://packagecloud.io/install/repositories/pagerduty/rundeck/script.deb.sh | os=any dist=any bash \
    && apt-get -y install rundeck-cli=${CLI_VERS}

RUN useradd -p $(echo "pampwd123" | openssl passwd -1 -stdin) -m --shell /bin/bash pamlogintest

    # RUNDECK - create user
RUN adduser --shell /bin/bash --home $HOME --gecos "" --disabled-password $USERNAME && \
        passwd -d $USERNAME && \
        addgroup $USERNAME sudo && \
        addgroup $USERNAME shadow

WORKDIR $HOME

COPY --chown=rundeck:rundeck data $HOME
COPY --chown=rundeck:rundeck api_test $HOME/api_test

ARG LAUNCHER_URL
ARG RUNDECK_NODE=rundeck1
RUN \
    java \
        -Dserver.http.port=4440 \
        -Dserver.hostname=$RUNDECK_NODE \
        -jar $HOME/rundeck-launcher.war --installonly \
    && rm -rf /var/lib/apt/lists/*

RUN apt-get -y update
RUN apt-get -y install dos2unix
USER rundeck


# Copy files.
RUN mkdir -p $HOME/scripts
COPY scripts $HOME/scripts
RUN sudo chmod -R a+x $HOME/scripts/*

RUN sudo mkdir -p /tests
COPY tests /tests
RUN sudo chmod -R a+x /tests/*

RUN sudo dos2unix $HOME/scripts/*.sh
RUN sudo dos2unix $HOME/api_test/api/*.sh
RUN sudo dos2unix $HOME/api_test/src/*.sh
RUN sudo dos2unix /tests/*.sh

VOLUME /var/lib/docker
VOLUME /test
VOLUME $HOME/resources

EXPOSE 22 4440 4443 4444

# Start the instance.
CMD $HOME/scripts/run.sh
