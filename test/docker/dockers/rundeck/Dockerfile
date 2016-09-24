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
        zip

## Install Oracle JVM
RUN \
  echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections && \
  add-apt-repository -y ppa:webupd8team/java && \
  apt-get update && \
  apt-get install -y oracle-java8-installer

## DEBUG ENV VARS AT THIS POINT
#RUN echo "**** ENV VARS START ****" && printenv > /env_at_build_time && cat /env_at_build_time && echo "**** ENV VARS END ****"

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


#download installer
ARG LAUNCHER_URL
ARG RUNDECK_NODE=rundeck1
RUN echo "download rundeck launcher: ${LAUNCHER_URL}"
RUN test -f $HOME/data/rundeck-launcher.jar && \
     cp $HOME/data/rundeck-launcher.jar $HOME/rundeck-launcher.jar || \
     true
RUN test -f $HOME/rundeck-launcher.jar || curl -sS -f -L -o $HOME/rundeck-launcher.jar ${LAUNCHER_URL}

# RUNDECK - install
RUN chown -R $USERNAME:$USERNAME $HOME
WORKDIR $HOME
USER rundeck

RUN java \
      -Dserver.http.port=4440 \
      -Dserver.hostname=$RUNDECK_NODE \
      -jar $HOME/rundeck-launcher.jar --installonly 

#      --java-opts "-Djava.security.egd=file:/dev/./urandom" 

EXPOSE 22 4440 4443

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

