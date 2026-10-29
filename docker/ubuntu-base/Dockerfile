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

# Build the JDK timezone database (tzdb.dat) from OpenJDK's tzdata
###################################################################
# Distro and vendor JDK builds still ship an older tzdb that misses the 2026
# Canadian time zone changes (Alberta, Northwest Territories, Manitoba), which
# would shift Quartz schedules in those zones by one hour from 2026-11-01.
# Compile tzdb.dat with OpenJDK's own build tool and data, using the same
# distro JDK as the image (the JDK package matching JRE_VERSION, for javac).
# The file is architecture independent, so this stage runs on the build platform.
# Remove this stage once the distro openjdk package ships tzdb >= 2026e.
FROM --platform=$BUILDPLATFORM ubuntu:22.04@sha256:5ec03bb3441e8b0bf3b4f9cd4629a1ae763010dc3035bb8da3ae6cf026486401 AS tzdb

ARG JRE_VERSION
RUN if [ -z "$JRE_VERSION" ]; then echo "Error: JRE_VERSION is not set"; exit 1; fi && \
    apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get -y --no-install-recommends install \
        ca-certificates curl "$(echo "$JRE_VERSION" | sed 's/-jre-/-jdk-/')" && \
    rm -rf /var/lib/apt/lists/*

# openjdk/jdk17u-dev commit carrying tzdata2026e
ENV JDK17U_COMMIT=d34607dc995ff876a4cff4c4d6777cb380e34149
ENV TZDATA_FILES="africa antarctica asia australasia europe northamerica southamerica backward etcetera gmt jdk11_backward"

WORKDIR /tzdb
RUN set -e; \
    base="https://raw.githubusercontent.com/openjdk/jdk17u-dev/${JDK17U_COMMIT}"; \
    mkdir -p data src out; \
    for f in VERSION $TZDATA_FILES; do curl -sSfL "$base/make/data/tzdata/$f" -o "data/$f"; done; \
    for f in TzdbZoneRulesCompiler TzdbZoneRulesProvider; do \
        curl -sSfL "$base/make/jdk/src/classes/build/tools/tzdb/$f.java" -o "src/$f.java"; done; \
    # Same copy step as OpenJDK's make/CopyInterimTZDB.gmk
    for f in ZoneRules ZoneOffsetTransition ZoneOffsetTransitionRule Ser; do \
        curl -sSfL "$base/src/java.base/share/classes/java/time/zone/$f.java" \
            | sed -e "s/package java.time.zone/package build.tools.tzdb/" > "src/$f.java"; done; \
    javac -nowarn -d out src/*.java; \
    cd data && java -cp ../out build.tools.tzdb.TzdbZoneRulesCompiler -srcdir . -dstfile /tzdb/tzdb.dat $TZDATA_FILES; \
    echo "Built tzdb.dat from $(tail -1 VERSION)"

# Build base container
######################
FROM ubuntu:22.04@sha256:5ec03bb3441e8b0bf3b4f9cd4629a1ae763010dc3035bb8da3ae6cf026486401

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

# Replace the JDK timezone database and fail the build if the zones are still stale
COPY --from=tzdb /tzdb/tzdb.dat /tmp/tzdb.dat
COPY TzdbCheck.java /tmp/TzdbCheck.java
RUN JAVA_HOME_DIR="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")" && \
    install -m 644 /tmp/tzdb.dat "$JAVA_HOME_DIR/lib/tzdb.dat" && \
    java /tmp/TzdbCheck.java && \
    rm /tmp/tzdb.dat /tmp/TzdbCheck.java

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
