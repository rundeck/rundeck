FROM ubuntu:16.04

## General package configuration
RUN apt-get -y update && \
    apt-get -y install \
        sudo \
        unzip \
        curl \
        xmlstarlet \
        git \
        netcat-traditional \
        software-properties-common \
        debconf-utils \
        uuid-runtime \
        ncurses-bin \
        iputils-ping \
        jq \
        zip \
        apt-transport-https \
        openjdk-8-jdk

# add cli tool debian repo
ARG CLI_DEB_URL
ARG CLI_VERS
RUN echo "deb $CLI_DEB_URL /" | sudo tee -a /etc/apt/sources.list

# add GPG key
#RUN curl "https://bintray.com/user/downloadSubjectPublicKey?username=rundeck" > /tmp/rundeck.gpg.key
#RUN apt-key add - < /tmp/rundeck.gpg.key
RUN curl "https://bintray.com/user/downloadSubjectPublicKey?username=bintray" > /tmp/bintray.gpg.key
RUN apt-key add - < /tmp/bintray.gpg.key

# RUNDECK

## RUNDECK setup env

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color

# RUNDECK - create user
RUN adduser --shell /bin/bash --home $HOME --gecos "" --disabled-password $USERNAME && \
    passwd -d $USERNAME && \
    addgroup $USERNAME sudo

WORKDIR $HOME

#RUN sed -i.bak -e "s|securerandom.source=file:/dev/random|securerandom.source=file:/dev/urandom|" /usr/lib/jvm/java-8-oracle/jre/lib/security/java.security

RUN mkdir -p $HOME/data
COPY data $HOME/data
VOLUME $HOME/data

#Install Rundeck CLI tool
RUN test -f $HOME/data/rd.deb && dpkg -i $HOME/data/rd.deb || true
RUN test -f $HOME/data/rd.deb || apt-get -y update
RUN test -f $HOME/data/rd.deb || apt-get -y install rundeck-cli=$CLI_VERS


#download installer
ARG LAUNCHER_URL
ARG RUNDECK_NODE=rundeck1
RUN echo "download rundeck launcher: ${LAUNCHER_URL}"
RUN test -f $HOME/data/rundeck-launcher.war && \
     cp $HOME/data/rundeck-launcher.war $HOME/rundeck-launcher.war || \
     true
RUN test -f $HOME/rundeck-launcher.war || curl -sS -f -L -o $HOME/rundeck-launcher.war ${LAUNCHER_URL}

# copy api test sources
RUN mkdir $HOME/api_test
COPY api_test $HOME/api_test

# RUNDECK - install
RUN chown -R $USERNAME:$USERNAME $HOME
WORKDIR $HOME
USER rundeck

RUN java \
      -Dserver.http.port=4440 \
      -Dserver.hostname=$RUNDECK_NODE \
      -jar $HOME/rundeck-launcher.war --installonly

#      --java-opts "-Djava.security.egd=file:/dev/./urandom" 

EXPOSE 22 4440 4443 4444

# Copy files.
#COPY config $HOME/config
RUN mkdir -p $HOME/scripts
COPY scripts $HOME/scripts
RUN sudo chmod -R a+x $HOME/scripts/*

RUN sudo mkdir -p /tests
COPY tests /tests
RUN sudo chmod -R a+x /tests/*

VOLUME /var/lib/docker
VOLUME /test
VOLUME $HOME/resources

# Start the instance.
CMD $HOME/scripts/run.sh

