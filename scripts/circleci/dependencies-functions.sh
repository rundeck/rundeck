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

 # Download and install Groovy 3.0.231
dependencies_install_groovy() {
    cd /tmp
    wget https://archive.apache.org/dist/groovy/3.0.23/distribution/apache-groovy-binary-3.0.23.zip
    sudo unzip apache-groovy-binary-3.0.23.zip -d /opt/
    sudo ln -sf /opt/groovy-3.0.23/bin/groovy /usr/local/bin/groovy
    sudo ln -sf /opt/groovy-3.0.23/bin/groovyc /usr/local/bin/groovyc
    sudo ln -sf /opt/groovy-3.0.23/bin/groovysh /usr/local/bin/groovysh
    export GROOVY_HOME=/opt/groovy-3.0.23
    export PATH=$GROOVY_HOME/bin:$PATH

    # Verify installation
    groovy --version
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
