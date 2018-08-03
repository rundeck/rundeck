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
        openjdk-8-jdk \
        uuid-runtime \
        wget \
    && rm -rf /var/lib/apt/lists/* \
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