#!/bin/bash
# Fail if a published Rundeck image still trusts a revoked CA root.
# Checks both trust stores from RUN-5117:
#   /etc/ssl/certs/ca-certificates.crt
#   /etc/ssl/certs/java/cacerts
#
# Usage: assert-revoked-ca-roots.sh <image>
# The fingerprint list is revoked-ca-roots.txt next to this script.
# Tests source this file and call revoked_ca_assert_stores.

REVOKED_CA_ROOTS_FILE="${REVOKED_CA_ROOTS_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/revoked-ca-roots.txt}"

revoked_ca_normalize_fp() {
    printf '%s' "$1" | tr -d '[:space:]:' | tr 'A-F' 'a-f'
}

revoked_ca_load_list() {
    local file="$1" line fp
    REVOKED_CA_FPS=()
    if [[ ! -s "$file" ]]; then
        echo "ERROR: revoked CA list is missing or empty: ${file}" >&2
        return 1
    fi
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        [[ "$line" =~ ^[[:space:]]*# ]] && continue
        [[ "$line" =~ ^[[:space:]]*$ ]] && continue
        fp=$(revoked_ca_normalize_fp "$line")
        if [[ ! "$fp" =~ ^[0-9a-f]{64}$ ]]; then
            echo "ERROR: invalid fingerprint in ${file}: ${line}" >&2
            return 1
        fi
        REVOKED_CA_FPS+=("$fp")
    done < "$file"
    if [[ ${#REVOKED_CA_FPS[@]} -eq 0 ]]; then
        echo "ERROR: revoked CA list has no fingerprints: ${file}" >&2
        return 1
    fi
}

revoked_ca_is_listed() {
    local fp="$1" known
    for known in "${REVOKED_CA_FPS[@]}"; do
        [[ "$fp" == "$known" ]] && return 0
    done
    return 1
}

# Write each PEM certificate in $pem to its own file under $dir.
revoked_ca_split_pem() {
    local pem="$1" dir="$2" line n=0 out=""
    while IFS= read -r line || [[ -n "$line" ]]; do
        line="${line%$'\r'}"
        if [[ "$line" == "-----BEGIN CERTIFICATE-----" ]]; then
            n=$((n + 1))
            out="${dir}/c$(printf '%03d' "$n").pem"
            : > "$out"
        fi
        if [[ -n "$out" ]]; then
            printf '%s\n' "$line" >> "$out"
        fi
        if [[ "$line" == "-----END CERTIFICATE-----" ]]; then
            out=""
        fi
    done < "$pem"
}

# Scan a PEM bundle. Sets REVOKED_CA_CERT_COUNT. Returns 1 when a listed root is present
# or when no certificate could be parsed.
revoked_ca_scan_pem() {
    local pem="$1" store_path="$2"
    local found=0 count=0 dir f fp subject
    dir=$(mktemp -d)
    revoked_ca_split_pem "$pem" "$dir"
    local matches=("${dir}"/c*.pem)
    if [[ -e "${matches[0]}" ]]; then
        for f in "${matches[@]}"; do
            fp=$(openssl x509 -in "$f" -noout -fingerprint -sha256 2>/dev/null | sed 's/^.*=//' | tr -d ':' | tr 'A-F' 'a-f') || true
            [[ -z "$fp" ]] && continue
            count=$((count + 1))
            if revoked_ca_is_listed "$fp"; then
                subject=$(openssl x509 -in "$f" -noout -subject 2>/dev/null | sed 's/^subject= *//')
                echo "ERROR: revoked CA root in ${store_path}" >&2
                echo "  sha256: ${fp}" >&2
                echo "  subject: ${subject:-unknown}" >&2
                found=1
            fi
        done
    fi
    rm -rf "$dir"
    REVOKED_CA_CERT_COUNT=$count
    if [[ "$count" -eq 0 ]]; then
        echo "ERROR: no certificates parsed from ${store_path}" >&2
        return 1
    fi
    [[ "$found" -eq 0 ]]
}

# Check the two trust-store files. label is included in the success line (image, platform).
revoked_ca_assert_stores() {
    local bundle="$1" keystore="$2" list="$3" label="${4:-}"
    local failed=0 bundle_count keystore_count pem err
    local suffix=""
    [[ -n "$label" ]] && suffix=" (${label})"

    if [[ ! -s "$bundle" ]]; then
        echo "ERROR: OS trust bundle is missing or empty${suffix}: /etc/ssl/certs/ca-certificates.crt" >&2
        return 1
    fi
    if [[ ! -s "$keystore" ]]; then
        echo "ERROR: JVM keystore is missing or empty${suffix}: /etc/ssl/certs/java/cacerts" >&2
        return 1
    fi
    revoked_ca_load_list "$list" || return 1

    revoked_ca_scan_pem "$bundle" "/etc/ssl/certs/ca-certificates.crt" || failed=1
    bundle_count=$REVOKED_CA_CERT_COUNT

    pem=$(mktemp)
    err=$(mktemp)
    if grep -q -- "-----BEGIN CERTIFICATE-----" "$keystore"; then
        cp "$keystore" "$pem"
    elif ! keytool -list -rfc -keystore "$keystore" -storepass changeit > "$pem" 2>"$err"; then
        echo "ERROR: could not read JVM keystore /etc/ssl/certs/java/cacerts${suffix}" >&2
        cat "$err" >&2
        rm -f "$pem" "$err"
        return 1
    fi
    revoked_ca_scan_pem "$pem" "/etc/ssl/certs/java/cacerts" || failed=1
    keystore_count=$REVOKED_CA_CERT_COUNT
    rm -f "$pem" "$err"

    if [[ "$failed" -ne 0 ]]; then
        return 1
    fi
    echo "OK: no revoked CA roots${suffix} (bundle=${bundle_count}, keystore=${keystore_count})"
}

revoked_ca_extract_stores() {
    local image="$1" platform="$2" dest="$3"
    local name="revoked-ca-check-$$-${RANDOM}"
    # Override the entrypoint so create does not require the image command and does not start Rundeck.
    local -a create_args=(create --name "$name" --entrypoint /)
    if [[ -n "$platform" ]]; then
        create_args=(create --platform "$platform" --name "$name" --entrypoint /)
    fi
    mkdir -p "$dest"
    docker "${create_args[@]}" "$image" unused >/dev/null
    local rc=0
    docker cp "$name:/etc/ssl/certs/ca-certificates.crt" "$dest/ca-certificates.crt" || rc=1
    docker cp "$name:/etc/ssl/certs/java/cacerts" "$dest/cacerts" || rc=1
    docker rm "$name" >/dev/null || true
    if [[ "$rc" -ne 0 ]]; then
        echo "ERROR: could not copy trust stores out of ${image}" >&2
        return 1
    fi
}

revoked_ca_assert_one_image() {
    local image="$1" platform="${2:-}"
    local dest label rc=0
    dest=$(mktemp -d)
    if ! revoked_ca_extract_stores "$image" "$platform" "$dest"; then
        rm -rf "$dest"
        return 1
    fi
    if [[ -n "$platform" ]]; then
        label="${image} ${platform}"
    else
        label="$image"
    fi
    if ! revoked_ca_assert_stores "$dest/ca-certificates.crt" "$dest/cacerts" "$REVOKED_CA_ROOTS_FILE" "$label"; then
        rc=1
    fi
    rm -rf "$dest"
    return "$rc"
}

revoked_ca_assert_image() {
    local image="${1:?image reference required}"
    if docker image inspect "$image" >/dev/null 2>&1; then
        revoked_ca_assert_one_image "$image"
        return
    fi

    # Pro multi-arch publishes the CI tag during buildx bake, before this step.
    # Pull each platform and block the later tag fan-out if either still trusts a revoked root.
    if [[ -n "${DOCKER_USERNAME:-}" && -n "${DOCKER_PASSWORD:-}" ]]; then
        echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin >/dev/null
    fi
    local platform rc=0
    for platform in linux/amd64 linux/arm64; do
        echo "Pulling ${image} (${platform})"
        docker pull --platform "$platform" "$image"
        revoked_ca_assert_one_image "$image" "$platform" || rc=1
    done
    return "$rc"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    set -euo pipefail
    if [[ $# -ne 1 || -z "${1:-}" ]]; then
        echo "Usage: $0 <image>" >&2
        exit 2
    fi
    revoked_ca_assert_image "$1"
fi
