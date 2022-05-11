# Build base container
######################
FROM ubuntu:20.04

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
        acl \
        curl \
        gnupg2 \
        ssh-client \
        sudo \
        openjdk-11-jdk-headless \
        uuid-runtime \
        wget \
        unzip \
    && rm -rf /var/lib/apt/lists/* \
    # Setup rundeck user
    && adduser --gid 0 --shell /bin/bash --home /home/rundeck --gecos "" --disabled-password rundeck \
    && chmod 0775 /home/rundeck \
    && passwd -d rundeck \
    && addgroup rundeck sudo \
    && chmod g+w /etc/passwd

# Add Tini
ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc /tini.asc
RUN gpg --batch --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 \
 && gpg --batch --verify /tini.asc /tini
RUN chmod +x /tini

RUN curl --request GET -sL \
    --url 'https://github.com/HeavyHorst/remco/releases/download/v0.12.3/remco_0.12.3_linux_amd64.zip'\
    --output 'remco.zip'
RUN echo '45f7073e02ce967e9bdc1e4f4a0b5c52b48a3085be4c2b9d04c912f839439c24  remco.zip' > remco.zip.sha
RUN sha256sum -c remco.zip.sha
RUN unzip remco.zip && cp remco_linux /usr/local/bin/remco

USER rundeck

WORKDIR /home/rundeck
