#!/bin/bash
set -euo pipefail

# Converts Snyk JSON output (from `snyk test --all-projects --json`) to JUnit XML
# for native display in CircleCI's Tests tab.
#
# Key behaviors:
# - Deduplicates findings by moduleName (package) across all scanned projects
# - Filters out license issues (snyk:lic: prefix), keeping only security vulnerabilities
# - Uses moduleName as classname for consistent CircleCI test tracking across runs
# - Aggregates all CVEs/CWEs/Snyk IDs for a package into one test case
# - Reports highest severity and highest CVSS score per package

readonly IN=${1:-/dev/stdin}

xml_escape() {
    local s="$1"
    s="${s//&/&amp;}"
    s="${s//</&lt;}"
    s="${s//>/&gt;}"
    s="${s//\"/&quot;}"
    s="${s//\'/&apos;}"
    echo "$s"
}

emit_empty() {
    echo '<?xml version="1.0" encoding="UTF-8"?><testsuites tests="0" failures="0"/>'
}

convert_snyk_junit() {
    local inputfile="$IN"

    # If reading from stdin, buffer to a temp file so we can read multiple times
    if [ "$inputfile" = "/dev/stdin" ]; then
        inputfile=$(mktemp)
        cat > "$inputfile"
    fi

    echo "input file: $IN" >&2

    if ! jq empty < "$inputfile" 2>/dev/null; then
        echo "ERROR: Invalid JSON in $IN" >&2
        emit_empty
        return 0
    fi

    if jq -e 'type == "object" and has("error")' < "$inputfile" > /dev/null 2>&1; then
        echo "Snyk scan had errors - generating empty JUnit output" >&2
        emit_empty
        return 0
    fi

    local tmpfile
    tmpfile=$(mktemp)
    trap "rm -f '$tmpfile'" EXIT

    # Flatten all vulnerabilities across projects, filter out license issues,
    # deduplicate by moduleName, and aggregate findings per package.
    jq -c '
      def sev_rank:
        if . == "critical" then 0
        elif . == "high" then 1
        elif . == "medium" then 2
        elif . == "low" then 3
        else 4 end;

      [ .[] | select(type == "object") | .vulnerabilities[]? ]
      | map(select(.id | startswith("snyk:lic:") | not))
      | group_by(.moduleName)
      | map({
          moduleName: .[0].moduleName,
          versions: ([.[].version] | unique | sort),
          snykIds: ([.[].id] | unique),
          cves: [.[].identifiers.CVE[]?] | unique | sort,
          cwes: [.[].identifiers.CWE[]?] | unique | sort,
          titles: ([.[].title] | unique),
          highestSeverity: ([ .[].severity ] | map({sev: ., rank: (. | sev_rank)}) | sort_by(.rank) | .[0].sev),
          maxCvssScore: ([.[].cvssScore // 0] | max),
          fixedIn: [.[].fixedIn[]?] | unique | sort,
          isUpgradable: ([.[].isUpgradable] | any),
          totalOccurrences: length
        })
      | sort_by((.highestSeverity | sev_rank), .moduleName)
    ' < "$inputfile" > "$tmpfile"

    local uniquePackages
    uniquePackages=$(jq 'length // 0' < "$tmpfile")
    uniquePackages=${uniquePackages:-0}
    local totalOccurrences
    totalOccurrences=$(jq '[.[].totalOccurrences] | add // 0' < "$tmpfile")
    totalOccurrences=${totalOccurrences:-0}

    echo "uniquePackages: $uniquePackages (from $totalOccurrences total occurrences across projects)" >&2

    echo '<?xml version="1.0" encoding="UTF-8"?>'
    echo "<testsuites tests=\"$uniquePackages\" failures=\"$uniquePackages\">"

    for sev in critical high medium low; do
        local sevPkgs
        sevPkgs=$(jq -c "[.[] | select(.highestSeverity == \"$sev\")]" < "$tmpfile")
        local sevCount
        sevCount=$(echo "$sevPkgs" | jq 'length')
        sevCount=${sevCount:-0}

        if [ "$sevCount" -eq 0 ]; then
            continue
        fi

        echo "  <testsuite name=\"Snyk: ${sev} severity\" tests=\"${sevCount}\" failures=\"${sevCount}\">"

        echo "$sevPkgs" | jq -c '.[]' | while IFS= read -r pkg; do
            local moduleName highestSev maxCvss isUpgradable totalOcc
            moduleName=$(echo "$pkg" | jq -r '.moduleName')
            highestSev=$(echo "$pkg" | jq -r '.highestSeverity')
            maxCvss=$(echo "$pkg" | jq -r '.maxCvssScore')
            isUpgradable=$(echo "$pkg" | jq -r '.isUpgradable')
            totalOcc=$(echo "$pkg" | jq -r '.totalOccurrences')

            local versions snykIds cves cwes titles fixedIn
            versions=$(echo "$pkg" | jq -r '.versions | join(", ")')
            snykIds=$(echo "$pkg" | jq -r '.snykIds | join(", ")')
            cves=$(echo "$pkg" | jq -r '.cves | join(", ")')
            cwes=$(echo "$pkg" | jq -r '.cwes | join(", ")')
            titles=$(echo "$pkg" | jq -r '.titles | join("; ")')
            fixedIn=$(echo "$pkg" | jq -r '.fixedIn | join(", ")')

            local idLabel=""
            if [ -n "$cves" ]; then
                idLabel="$cves"
            elif [ -n "$cwes" ]; then
                idLabel="$cwes"
            else
                idLabel="$snykIds"
            fi

            local sevUpper
            sevUpper=$(echo "$highestSev" | tr '[:lower:]' '[:upper:]')

            local escapedName escapedClass escapedTitle
            escapedName=$(xml_escape "${sevUpper} ${moduleName} (${totalOcc} findings) ${idLabel}")
            escapedClass=$(xml_escape "$moduleName")
            escapedTitle=$(xml_escape "$titles")

            cat <<END
    <testcase name="${escapedName}" classname="${escapedClass}">
      <failure message="${escapedTitle}">
<![CDATA[
Highest Severity: ${highestSev}
Max CVSS Score: ${maxCvss}
Issues: ${titles}
CVEs: ${cves:-None}
CWEs: ${cwes:-None}
Package: ${moduleName}
Affected Versions: ${versions}
Fixed In: ${fixedIn:-No fix available}
Upgradable: ${isUpgradable}
Occurrences: ${totalOcc} (across dependency paths/projects)
Snyk IDs: ${snykIds}
]]>
      </failure>
    </testcase>
END
        done

        echo "  </testsuite>"
    done

    if [ "$uniquePackages" -eq 0 ]; then
        echo "  <testsuite name=\"Snyk: security scan\" tests=\"1\" failures=\"0\">"
        echo "    <testcase name=\"No security vulnerabilities found\" classname=\"snyk.scan\"/>"
        echo "  </testsuite>"
    fi

    echo "</testsuites>"
}

convert_snyk_junit
