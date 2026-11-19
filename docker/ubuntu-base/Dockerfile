# Build remco from org fork with CVE fixes applied
###################################################
FROM golang:1.27.3@sha256:f20d83a83e18dbd2a4714b8ffd4b30b287b6243962dd75608e86abcc63ba4efc AS remco

# Pinned to a rundeck/remco commit synced with upstream HeavyHorst/remco.
# Fixes golang.org/x/crypto 0.52.0, golang.org/x/net 0.55.0, and
# google.golang.org/grpc 1.82.1 - see RUN-4857 for the full CVE list.
# Go 1.27.3+ required for CVE-2026-111943 (eMudhra intermediate CA denylist).
ENV REMCO_COMMIT=bbc9243fec2e05c2fc8bf8a66bd9e24f04a2762b

RUN git clone https://github.com/rundeck/remco.git /tmp/remco && \
    cd /tmp/remco && \
    git checkout $REMCO_COMMIT && \
    GOTOOLCHAIN=auto go build -o /go/bin/remco ./cmd/remco

# Build base container
######################
FROM ubuntu:22.04@sha256:076e7bf82319619f28dd0aa9c02ab34b055ad37100395b4f49a2ffba04fb5e11

ARG TARGETARCH

# openjdk-17-jre-headless is the historical default CI still passes. That value
# installs pinned Azul Zulu 17.70.201 (OpenJDK 17.0.21.1) instead of Ubuntu's
# OpenJDK 17.0.21.0.3, which is affected by CVE-2026-114938. Zulu 17.0.21.1
# also ships IANA tzdata 2026e, so the Canadian zone overlay is no longer built.
# Any other value is an Ubuntu apt package name (the JDK 25 CI image).
ARG JRE_VERSION=openjdk-17-jre-headless

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
        ca-certificates \
        curl \
        gnupg2 \
        ssh-client \
        sudo \
        uuid-runtime \
        wget \
        unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Install the runtime. See JRE_VERSION above for why the default is Zulu.
RUN set -eux; \
    if [ -z "$JRE_VERSION" ]; then echo "Error: JRE_VERSION is not set"; exit 1; fi; \
    if [ "$JRE_VERSION" = "openjdk-17-jre-headless" ]; then \
        mkdir -p /usr/share/keyrings; \
        curl -sSfL https://repos.azul.com/azul-repo.key | gpg --batch --dearmor -o /usr/share/keyrings/azul.gpg; \
        echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" > /etc/apt/sources.list.d/zulu.list; \
        apt-get update; \
        DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends install \
            zulu17-jre-headless=17.0.21.1-1; \
        apt-mark hold zulu17-jre-headless zulu17-ca-jre-headless; \
    else \
        apt-get update; \
        DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends install "$JRE_VERSION"; \
    fi; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*

# Fail the build if this JRE still has a DST transition for the Canadian zones
# that moved to permanent time (would shift Quartz schedules by one hour).
COPY TzdbCheck.java /tmp/TzdbCheck.java
RUN java /tmp/TzdbCheck.java && rm /tmp/TzdbCheck.java

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
