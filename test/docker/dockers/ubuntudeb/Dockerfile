FROM ubuntu:16.04

## General package configuration
RUN apt-get -y update && \
    apt-get -y install \
        sudo \
        debconf-utils \
        apt-utils \
        apt-transport-https \
        wget

RUN echo "deb https://dl.bintray.com/rundeck/rundeck-deb /" | sudo tee -a /etc/apt/sources.list

RUN wget -qO - https://bintray.com/user/downloadSubjectPublicKey?username=bintray | sudo apt-key add -

RUN sudo apt-get -y update 

## install openjdk 8
RUN sudo apt-get install -y openjdk-8-jdk

## or:
## Install Oracle JVM
#RUN \
#  echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections && \
#  add-apt-repository -y ppa:webupd8team/java && \
#  apt-get update && \
#  apt-get install -y oracle-java8-installer

## DEBUG ENV VARS AT THIS POINT
#RUN echo "**** ENV VARS START ****" && printenv > /env_at_build_time && cat /env_at_build_time && echo "**** ENV VARS END ****"

# RUNDECK

## RUNDECK setup env

ENV USERNAME=rundeck \
    USER=rundeck \
    HOME=/home/rundeck \
    LOGNAME=$USERNAME \
    TERM=xterm-256color
#
# RUNDECK - create user
RUN adduser --shell /bin/bash --home $HOME --gecos "" --disabled-password $USERNAME && \
    passwd -d $USERNAME && \
    addgroup $USERNAME sudo

COPY run.sh $HOME/run.sh
RUN sudo chmod +x $HOME/run.sh

RUN chown -R $USERNAME:$USERNAME $HOME
WORKDIR $HOME
USER rundeck

#download debian package
RUN  sudo apt-get install -y rundeck

# RUNDECK - install

EXPOSE 22 4440 4443

# Start the instance.
CMD $HOME/run.sh

