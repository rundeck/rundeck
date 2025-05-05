# Build remco from specific commit
##################################
FROM golang:1.24.2 as remco

# REMCO_VERSION can be a release version "v0.XX.X", commit id, tag or branch name.
ENV REMCO_VERSION=fe0db414ca99ce5055ceb316f52a13948b213d36

RUN go install github.com/HeavyHorst/remco/cmd/remco@$REMCO_VERSION

# Build base container
######################
FROM ubuntu:22.04

ARG TARGETARCH

# The required JRE version args
ARG JRE_VERSION
RUN echo "Image JRE: ${JRE_VERSION}"
RUN if [ -z "$JRE_VERSION" ]; then echo "Error: JRE_VERSION is not set"; exit 1; fi

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
        $JRE_VERSION \
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
RUN curl -sSL https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini-${TARGETARCH} -o /tini && \
    curl -sSL https://github.com/krallin/tini/releases/download/v${TINI_VERSION}/tini-${TARGETARCH}.asc -o /tini.asc && \
    gpg --batch --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 && \
    gpg --batch --verify /tini.asc /tini && \
    chmod +x /tini

# Install Remco
COPY --from=remco /go/bin/remco /usr/local/bin/remco

USER rundeck

WORKDIR /home/rundeck
