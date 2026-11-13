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
    "${target}/bin/java" -version 2>&1 | grep -q 'version "17'
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

# Refuse a JDK that still trusts the roots that signed the compromised eMudhra intermediates.
# Temurin October 2026 builds (17.0.21+9, 25.0.5+8) remove those roots from cacerts.
# A JDK that still has the roots is accepted only when blocked.certs lists them.
dependencies_assert_emudhra_untrusted() {
    local java_home="$1"
    local cacerts="${java_home}/lib/security/cacerts"
    local blocked
    for blocked in "${java_home}/lib/security/blocked.certs" "${java_home}/conf/security/blocked.certs"; do
        if [[ -f "${blocked}" ]] && grep -qiE 'emudhra|emSign ECC Root CA - G3|AAA Certificate Services' "${blocked}"; then
            echo "JDK blocklist covers the eMudhra intermediates: ${blocked}"
            return 0
        fi
    done
    if [[ ! -f "${cacerts}" ]]; then
        echo "No cacerts at ${cacerts}"
        exit 1
    fi
    local dump
    if ! dump="$("${java_home}/bin/keytool" -list -v -keystore "${cacerts}" -storepass changeit)"; then
        echo "keytool failed reading ${cacerts}"
        exit 1
    fi
    local bad=0
    if echo "${dump}" | grep -q 'CN=emSign ECC Root CA - G3'; then
        echo "JDK still trusts emSign ECC Root CA - G3 (${java_home})"
        bad=1
    fi
    if echo "${dump}" | grep -q 'CN=AAA Certificate Services'; then
        echo "JDK still trusts AAA Certificate Services (${java_home})"
        bad=1
    fi
    if [[ "${bad}" -ne 0 ]]; then
        echo "Refusing ${java_home}: cacerts still trusts the eMudhra signing roots and blocked.certs does not list them."
        exit 1
    fi
    echo "JDK cacerts does not trust the eMudhra signing roots (${java_home})"
}

dependencies_jdk_home() {
    dirname "$(dirname "$(readlink -f "$(command -v java)")")"
}

# Select JDK 17 from the Build workspace (.ci-jdk) or from the executor image.
# Does not install a JDK. An unpinned apt package can still ship the compromised roots.
dependencies_install_zulu17jdk() {
      local java_home
      local persisted_jdk
      persisted_jdk="$(ci_jdk_dir)"
      if [[ -x "${persisted_jdk}/bin/javac" ]]; then
        echo "Using JDK 17 persisted by the Build job: ${persisted_jdk}"
        if [[ -n "${BASH_ENV:-}" ]]; then
          echo "export JAVA_HOME=${persisted_jdk}" >> "${BASH_ENV}"
          echo "export PATH=\"${persisted_jdk}/bin:\${PATH}\"" >> "${BASH_ENV}"
        fi
        export JAVA_HOME="${persisted_jdk}"
        export PATH="${persisted_jdk}/bin:${PATH}"
        java_home="${persisted_jdk}"
      elif java -version 2>&1 | grep -q 'version "17' && javac -version >/dev/null 2>&1; then
        echo "Using JDK 17 from the executor image"
        java_home="$(dependencies_jdk_home)"
        export JAVA_HOME="${java_home}"
      else
        echo "JDK 17 not found. Docker jobs must use the pinned cimg/openjdk image. Machine jobs must attach the Build workspace (.ci-jdk) before this step. Refusing to apt-install an unpinned JDK."
        exit 1
      fi
      java -version
      javac -version
      dependencies_assert_emudhra_untrusted "${java_home}"
}

# Select JDK 25 from the executor image (docker-builder-25). Does not install a JDK.
# Writes JAVA_HOME + PATH into BASH_ENV so later Circle steps use this JDK.
dependencies_install_zulu25jdk() {
      if ! java -version 2>&1 | grep -q 'version "25' || ! javac -version >/dev/null 2>&1; then
        echo "JDK 25 is not on this executor. Run the job on docker-builder-25. Refusing to apt-install an unpinned JDK."
        exit 1
      fi
      local java_home
      java_home="$(dependencies_jdk_home)"
      echo "Using JDK 25 from the executor image: ${java_home}"
      if [[ -n "${BASH_ENV:-}" ]]; then
        echo "export JAVA_HOME=${java_home}" >> "${BASH_ENV}"
        echo "export PATH=\"${java_home}/bin:\${PATH}\"" >> "${BASH_ENV}"
      fi
      export JAVA_HOME="${java_home}"
      export PATH="${java_home}/bin:${PATH}"
      java -version
      javac -version
      dependencies_assert_emudhra_untrusted "${java_home}"
}


# Install dependencies needed for packaging
dependencies_packaging_setup() {

    apt_get_retry \
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

    apt_get_retry \
        xmlstarlet \
        file \
        dpkg \
        jq
}
