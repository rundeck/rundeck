# Build remco from specific commit
##################################
FROM golang

RUN go get github.com/HeavyHorst/remco/cmd/remco
RUN cd $GOPATH/src/github.com/HeavyHorst/remco && \
    git checkout 316b72d05ff25448ae66d0ecdbd6e3d9393a80e3
RUN go install github.com/HeavyHorst/remco/cmd/remco

# Build base container
######################
FROM ubuntu:16.04

ENV DEBIAN_FRONTEND noninteractive
ENV LANG C.UTF-8
ENV LC_ALL C.UTF-8

## BASH
RUN echo "dash dash/sh boolean false" | debconf-set-selections \
    && dpkg-reconfigure dash

## General package configuration
RUN set -euxo pipefail \
    && sed -i -e 's#http://\(archive\|security\)#mirror://mirrors#' -e 's#/ubuntu/#/mirrors.txt#' /etc/apt/sources.list \
    && apt-get -y update && apt-get -y --no-install-recommends install \
        curl \
        gnupg2 \
        ssh-client \
        sudo \
        openjdk-8-jdk-headless \
        uuid-runtime \
        wget \
    && rm -rf /var/lib/apt/lists/* \
    # Setup rundeck user
    && adduser --shell /bin/bash --home /home/rundeck --gecos "" --disabled-password rundeck \
    && passwd -d rundeck \
    && addgroup rundeck sudo \
    && echo | sudo -u rundeck ssh-keygen -N '' \
    && echo 'PATH=$PATH:$HOME/tools/bin' >> /home/rundeck/.bashrc

# Add Tini
ENV TINI_VERSION v0.18.0
RUN wget -O /tini https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini \
    && wget -O /tini.asc https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc \
    && gpg --batch --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 \
    && gpg --batch --verify /tini.asc /tini \
    && chmod +x /tini

COPY --from=0 /go/bin/remco /usr/local/bin/remco

USER rundeck

WORKDIR /home/rundeck