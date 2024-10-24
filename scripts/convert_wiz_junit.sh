#!/bin/bash
set -euo pipefail

# Path to the input JSON file from wizcli scan output
readonly IN=${1:-/dev/stdin}

# Convert wizcli scan results to JUnit XML format
convert_wiz_junit() {
    # Counts of vulnerabilities by severity
    echo "input file: $IN"
    local time=$(jq -r '.createdAt' < "$IN" | cut -d'T' -f1)
    local totalReportedVulnerabilities=$(jq '[.result.osPackages[]?.vulnerabilities[]?, .result.libraries[]?.vulnerabilities[]?] | length' < "$IN")
    echo "totalVulnsReported: $totalReportedVulnerabilities"
    local reportUrl=$(jq -r '.reportUrl' < "$IN")

    cat <<END
<?xml version="1.0" encoding="UTF-8"?>
<testsuites failures="$totalReportedVulnerabilities" tests="$totalReportedVulnerabilities" timestamp="$time">
  <testsuite name="Wiz Scan Vulnerabilities" tests="$totalReportedVulnerabilities" failures="$totalReportedVulnerabilities">
END

    # Concatenate vulnerabilities from osPackages and libraries
    jq -c '.result.osPackages[]?, .result.libraries[]? | . as $pkg | ($pkg.vulnerabilities[]? | . + {packageName: $pkg.name, packageVersion: $pkg.version, packagePath: $pkg.path})' < "$IN" |
    while IFS= read -r vuln; do
        local name=$(echo "$vuln" | jq -r '.name')
        local severity=$(echo "$vuln" | jq -r '.severity')
        local description=$(echo "$vuln" | jq -r '.description // "No description provided"')
        local link=$(echo "$vuln" | jq -r '.source // "No source provided"')
        local packageName=$(echo "$vuln" | jq -r '.packageName')
        local packageVersion=$(echo "$vuln" | jq -r '.packageVersion')
        local packagePath=$(echo "$vuln" | jq -r '.packagePath')
        local fixedVersion=$(echo "$vuln" | jq -r '.fixedVersion')

        cat <<END
    <testcase name="${packageName}:${packageVersion} - ${severity}" classname="${name}" severity="${severity}" file="${packagePath}">
      <failure message="${name}">
<![CDATA[
Severity: ${severity}
Package: ${packageName}
Current Version: ${packageVersion}
Fixed Versions: ${fixedVersion}
Path: ${packagePath}
Link: ${link}
Wiz Report: ${reportUrl}
Description:

${description}
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
