#!/bin/bash
set -e

# Install Azul Zulu JDK 11 (legacy)
dependencies_install_zulu11jdk() {
      # Azul Zulu JDK Install
      sudo apt-get update
      sudo apt install gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      sudo apt-get update
      sudo apt-get -y --no-install-recommends install zulu11-jdk-headless

}

# Install Azul Zulu JDK 17 (for Grails 7)
dependencies_install_zulu17jdk() {
      # Azul Zulu JDK 17 Install
      sudo apt-get update
      sudo apt install gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      sudo apt-get update
      sudo apt-get -y --no-install-recommends install zulu17-jdk-headless

}

# Install Azul Zulu JDK 25 (forward-compat / newer-LTS testing on CI host).
# Writes JAVA_HOME + PATH into BASH_ENV so later Circle steps use this JDK.
dependencies_install_zulu25jdk() {
      sudo apt-get update
      sudo apt install gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      sudo apt-get update
      sudo apt-get -y --no-install-recommends install zulu25-jdk-headless

      local java_home=""
      for candidate in /usr/lib/jvm/zulu25-ca-amd64 /usr/lib/jvm/zulu25-amd64 /usr/lib/jvm/zulu25; do
        if [[ -x "${candidate}/bin/java" ]]; then
          java_home="${candidate}"
          break
        fi
      done
      if [[ -z "${java_home}" ]]; then
        echo "Could not find Zulu 25 under /usr/lib/jvm; listing:"
        ls -la /usr/lib/jvm || true
        exit 1
      fi
      echo "Using JAVA_HOME=${java_home}"
      "${java_home}/bin/java" -version
      if [[ -n "${BASH_ENV:-}" ]]; then
        echo "export JAVA_HOME=${java_home}" >> "${BASH_ENV}"
        echo "export PATH=\"${java_home}/bin:\${PATH}\"" >> "${BASH_ENV}"
      fi
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
