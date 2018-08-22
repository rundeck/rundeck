# Build remco from specific commit
##################################
FROM golang

RUN go get github.com/HeavyHorst/remco/cmd/remco
RUN cd $GOPATH/src/github.com/HeavyHorst/remco && \
    git checkout ec5c624a154f175f82f42eeda8e687d8915cfc35
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
    && echo | sudo -u rundeck ssh-keygen -N ''

COPY --from=0 /go/bin/remco /usr/local/bin/remco

USER rundeck

WORKDIR /home/rundeck