# Build base container
######################
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

# Set Bash as default shell
RUN echo "dash dash/sh boolean false" | debconf-set-selections && \
    dpkg-reconfigure dash

# General package configuration
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get -y --no-install-recommends install \
        acl \
        curl \
        gnupg2 \
        ssh-client \
        sudo \
        openjdk-11-jre-headless \
        uuid-runtime \
        wget \
        unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Setup rundeck user
RUN adduser --gid 0 --shell /bin/bash --home /home/rundeck --gecos "" --disabled-password rundeck && \
    chmod 0775 /home/rundeck && \
    passwd -d rundeck && \
    addgroup rundeck sudo && \
    chmod g+w /etc/passwd

# Install Tini
ENV TINI_VERSION=0.19.0
RUN curl -sSL https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini -o /tini && \
    curl -sSL https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini.asc -o /tini.asc && \
    gpg --batch --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 && \
    gpg --batch --verify /tini.asc /tini && \
    chmod +x /tini

# Install Remco
ENV REMCO_VERSION=0.12.4
RUN curl -ssL "https://github.com/HeavyHorst/remco/releases/download/v${REMCO_VERSION}/remco_${REMCO_VERSION}_linux_amd64.zip" -o remco.zip && \
    echo 'c7eae0db823660f8cf0c0ccba1beebec64ee1c2e5b6b66ca3d20208cbcb70962  remco.zip' > remco.zip.sha && \
    sha256sum -c remco.zip.sha && \
    unzip remco.zip && \
    cp remco_linux /usr/local/bin/remco && \
    chmod +x /usr/local/bin/remco && \
    rm remco.zip remco.zip.sha

USER rundeck

WORKDIR /home/rundeck
