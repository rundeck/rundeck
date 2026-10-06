#!/bin/bash
set -e

# Runs "apt-get update && apt-get install" with retries to survive transient Ubuntu mirror errors.
# Usage: apt_get_retry <package>...
# Retries up to 3 times, APT_RETRY_DELAY seconds apart (default 15). Returns 1 after the last failure.
apt_get_retry() {
    local max_attempts=3
    local attempt
    for attempt in $(seq 1 "${max_attempts}"); do
        if sudo apt-get update && sudo apt-get -y --no-install-recommends install "$@"; then
            return 0
        fi
        echo "apt-get attempt ${attempt}/${max_attempts} failed for: $*"
        if [[ "${attempt}" -lt "${max_attempts}" ]]; then
            sleep "${APT_RETRY_DELAY:-15}"
        fi
    done
    return 1
}

# Directory (inside the workspace) where the Build job persists its JDK for the downstream jobs.
ci_jdk_dir() {
    echo "${WORKDIR:-${HOME}/workspace}/.ci-jdk"
}

# Copies the JDK running this job to the workspace so downstream jobs don't have to install one with apt.
# Symlinks are dereferenced so the copy is self-contained (e.g. cacerts pointing to /etc/ssl).
dependencies_persist_jdk() {
    local javac_path jdk_home target
    javac_path="$(readlink -f "$(command -v javac)")"
    jdk_home="$(dirname "$(dirname "${javac_path}")")"
    target="$(ci_jdk_dir)"
    echo "Persisting JDK ${jdk_home} to ${target}"
    rm -rf "${target}"
    cp -aL "${jdk_home}" "${target}"
    "${target}/bin/java" -version 2>&1 | grep -q 'version "25'
    "${target}/bin/javac" -version
}

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

# Install JDK 21 (skips if already provided by the Docker image, e.g. cimg/openjdk:21.0).
# Grails 8 / Gradle 9 require a JDK 21 build JVM.
dependencies_install_zulu21jdk() {
      if java -version 2>&1 | grep -q 'version "21' && javac -version >/dev/null 2>&1; then
        echo "JDK 21 already installed — skipping apt install"
        java -version
        javac -version
        return 0
      fi

      echo "JDK 21 not found — installing Azul Zulu JDK 21 via apt"
      sudo apt-get update
      sudo apt install gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      sudo apt-get update
      sudo apt-get -y --no-install-recommends install zulu21-jdk-headless

      local java_home=""
      for candidate in /usr/lib/jvm/zulu21-ca-amd64 /usr/lib/jvm/zulu21-amd64 /usr/lib/jvm/zulu21; do
        if [[ -x "${candidate}/bin/java" ]]; then
          java_home="${candidate}"
          break
        fi
      done
      if [[ -z "${java_home}" ]]; then
        echo "Could not find Zulu 21 under /usr/lib/jvm; listing:"
        ls -la /usr/lib/jvm || true
        exit 1
      fi
      echo "Using JAVA_HOME=${java_home}"
      "${java_home}/bin/java" -version
      if [[ -n "${BASH_ENV:-}" ]]; then
        echo "export JAVA_HOME=${java_home}" >> "${BASH_ENV}"
        echo "export PATH=\"${java_home}/bin:\${PATH}\"" >> "${BASH_ENV}"
      fi
      export JAVA_HOME="${java_home}"
}

# Install Azul Zulu JDK 25 (forward-compat / newer-LTS testing on CI host).
# Writes JAVA_HOME + PATH into BASH_ENV so later Circle steps use this JDK.
dependencies_install_zulu25jdk() {
      apt_get_retry gnupg ca-certificates curl
      curl -s https://repos.azul.com/azul-repo.key | sudo gpg --dearmor -o /usr/share/keyrings/azul.gpg
      echo "deb [signed-by=/usr/share/keyrings/azul.gpg] https://repos.azul.com/zulu/deb stable main" | sudo tee /etc/apt/sources.list.d/zulu.list

      apt_get_retry zulu25-jdk-headless

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
