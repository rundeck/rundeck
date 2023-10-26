#!/bin/bash
set -e

# Install dependencies needed for building
dependencies_build_setup() {

    sudo apt-get update
    sudo apt-get -y --no-install-recommends install \
        openjdk-11-jdk-headless

    # Node and aws installed by orb.

    # Install groovy
    GROOVY_VERSION=3.0.19
    curl -sSL -o /tmp/groovy.zip "https://archive.apache.org/dist/groovy/${GROOVY_VERSION}/distribution/apache-groovy-binary-${GROOVY_VERSION}.zip" &&
        echo '6ff5e1fde0ca7dbea06bc5241502574014a64af734c4910acdb4218b4c504230 /tmp/groovy.zip' | sha256sum --check &&
        unzip /tmp/groovy.zip -d /tmp &&
        rm -f /tmp/groovy.zip &&
        sudo mv "/tmp/groovy-${GROOVY_VERSION}" "/usr/local/groovy" &&
        sudo ln --symbolic "/usr/local/groovy/bin/groovy" /usr/local/bin/groovy &&
        sudo ln --symbolic "/usr/local/groovy/bin/groovysh" /usr/local/bin/groovysh &&
        sudo ln --symbolic "/usr/local/groovy/bin/groovyc" /usr/local/bin/groovyc

    mkdir -p "$HOME/.gradle"
}

# Install dependencies needed for packaging
dependencies_packaging_setup() {

    dependencies_build_setup

    sudo apt-get -y --no-install-recommends install \
        openjdk-11-jdk-headless \
        dpkg \
        xmlstarlet \
        expect \
        rpm \
        dpkg-sig \
        gnupg2 \
        ruby-dev

}

# Install dependencies needed for testdeck
dependencies_testdeck_setup() {

    sudo apt-get update
    sudo apt-get -y --no-install-recommends install \
        xmlstarlet \
        openjdk-11-jdk-headless \
        file \
        dpkg \
        jq

}
