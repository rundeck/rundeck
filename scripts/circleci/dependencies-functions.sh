#!/bin/bash
set -e

# Install Azul Zulu JDK
dependencies_install_zulu11jdk() {
      # Azul Zulu JDK Install
      sudo apt-get update
      sudo apt install gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      sudo apt-get update
      sudo apt-get -y --no-install-recommends install zulu11-jdk-headless

}

# Install groovy SDK
dependencies_install_groovy() {
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

}

# Install nodejs
dependencies_install_nodejs() {
  node_version=$(cat .nvmrc)
  curl -sSl -o /tmp/node.tar.xz "https://nodejs.org/dist/${node_version}/node-${node_version}-linux-x64.tar.xz" && \
  echo '592eb35c352c7c0c8c4b2ecf9c19d615e78de68c20b660eb74bd85f8c8395063 /tmp/node.tar.xz' | sha256sum --check && \
  sudo mkdir -p /usr/local/nodejs && \
  sudo tar --strip 1 -xvf /tmp/node.tar.xz --directory /usr/local/nodejs && \
  sudo ln --symbolic /usr/local/nodejs/bin/node /usr/local/bin/node && \
  sudo ln --symbolic /usr/local/nodejs/bin/npm /usr/local/bin/npm && \
  sudo ln --symbolic /usr/local/nodejs/bin/npx /usr/local/bin/npx && \
  sudo ln --symbolic /usr/local/nodejs/bin/corepack /usr/local/bin/corepack
  rm -f /tmp/node.tar.xz
}


# Install dependencies needed for packaging
dependencies_packaging_setup() {

    sudo apt-get update
    sudo apt-get -y --no-install-recommends install \
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
        file \
        dpkg \
        jq
}
