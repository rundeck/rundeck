#!/bin/bash
set -euo pipefail

# Path to the input JSON file from wizcli scan output
readonly IN=${1:-/dev/stdin}

# Convert wizcli scan results to JUnit XML format
convert_wiz_junit() {
    # Counts of vulnerabilities by severity
    echo "input file: $IN"
    local lowCount=$(jq -r '.result.analytics.vulnerabilities.lowCount // 0' < "$IN")
    local mediumCount=$(jq -r '.result.analytics.vulnerabilities.mediumCount // 0' < "$IN")
    local highCount=$(jq -r '.result.analytics.vulnerabilities.highCount // 0' < "$IN")
    local criticalCount=$(jq -r '.result.analytics.vulnerabilities.criticalCount // 0' < "$IN")

    local time=$(jq -r '.createdAt' < "$IN" | cut -d'T' -f1)

    local totalCount=$((lowCount + mediumCount + highCount + criticalCount))

    cat <<END
<?xml version="1.0" encoding="UTF-8"?>
<testsuites failures="$((lowCount + mediumCount))" tests="$totalCount" timestamp="$time">
  <testsuite name="Wiz Scan Vulnerabilities" tests="$totalCount" failures="$((lowCount + mediumCount))">
END

    # Concatenate vulnerabilities from osPackages and libraries, then filter for high and critical
    jq -c '.result.osPackages[]?, .result.libraries[]? | . as $pkg | ($pkg.vulnerabilities[]? | select(.severity == "HIGH" or .severity == "CRITICAL") | . + {packageName: $pkg.name, packageVersion: $pkg.version})' < "$IN" |
    while IFS= read -r vuln; do
        local name=$(echo "$vuln" | jq -r '.name')
        local severity=$(echo "$vuln" | jq -r '.severity')
        local description=$(echo "$vuln" | jq -r '.description // "No description provided"')
        local link=$(echo "$vuln" | jq -r '.source // "No source provided"')
        local packageName=$(echo "$vuln" | jq -r '.packageName')
        local packageVersion=$(echo "$vuln" | jq -r '.packageVersion')

        cat <<END
    <testcase name="${packageName} ${packageVersion}: ${name}" severity="${severity}" link="${link}">
      <failure message="Severity: ${severity}">
<![CDATA[
Package: ${packageName}
Version: ${packageVersion}
Description: ${description}
Link: ${link}
]]>
      </failure>
    </testcase>
END
    done

    cat <<END
  </testsuite>
</testsuites>
END
}

convert_wiz_junit
