# Build remco from org fork with CVE fixes applied
###################################################
FROM golang:1.27.0 AS remco

# Pinned to rundeck/remco @ HEAD (synced to upstream HeavyHorst/remco).
# Includes fixes through this commit: golang.org/x/crypto 0.52.0,
# golang.org/x/net 0.55.0, google.golang.org/grpc 1.82.1 - addresses
# CVE-2026-39827 through -39835, CVE-2026-42508, CVE-2026-46595,
# CVE-2026-46597, CVE-2026-46598, CVE-2026-25680, CVE-2026-39821,
# and GHSA-hrxh-6v49-42gf.
ENV REMCO_COMMIT=bbc9243fec2e05c2fc8bf8a66bd9e24f04a2762b

RUN git clone https://github.com/rundeck/remco.git /tmp/remco && \
    cd /tmp/remco && \
    git checkout $REMCO_COMMIT && \
    GOTOOLCHAIN=auto go build -o /go/bin/remco ./cmd/remco

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
