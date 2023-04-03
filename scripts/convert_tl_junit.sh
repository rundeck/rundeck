#!/bin/bash
set -euo pipefail
readonly ARGS=("$@")
IN=${ARGS[0]:-/dev/stdin}
THRESHOLD=${THRESHOLD:-2}

#/ use jq to convert twistlock scan into junit xml
convert_tl_to_junit(){
	local total=$(jq -r '.results[0].vulnerabilityDistribution.total' < "$IN")
	local low=$(jq -r '.results[0].vulnerabilityDistribution.low' < "$IN")
	local medium=$(jq -r '.results[0].vulnerabilityDistribution.medium' < "$IN")
	local high=$(jq -r '.results[0].vulnerabilityDistribution.high' < "$IN")
	local critical=$(jq -r '.results[0].vulnerabilityDistribution.critical' < "$IN")

	local sevcount=($low $medium $high $critical)

	local time=$(jq -r '.results[0].scanTime' < "$IN")

	local sum=0

	for c in $(seq "$THRESHOLD" 3) ; do
		sum=$(( $sum + ${sevcount[$c]} ))
	done

	local sevs=("low" "medium" "high" "critical")

	cat <<END
<?xml version="1.0" encoding="UTF-8"?>
<testsuites failures="$sum" name="twistlock_scan" tests="$total">
END

	for sev in "${sevs[@]:$THRESHOLD}" ; do
		local count=$(jq -r ".results[0].vulnerabilityDistribution.$sev" < "$IN")
		if [[ $count -gt 0 ]]; then

		  cat <<END
<testsuite failures="${count}" errors="0" time="0" id="1" name="severity $sev" package="twistlock" skipped="0" tests="$count" timestamp="$time">
END
      # iterate over vulnerabilities with the right severity
      # then emit testcase xml element for each one
      jq -a ".results[0].vulnerabilities | map(select(.severity == \"$sev\"))" < "$IN"  |
        jq -r 'map("<testcase classname=\"" + (.id | @html) + "\" name=\"" + (.packageName | @html) + "\">\n<failure message=\"cvss: " + (.cvss?|tostring|@html) + " vector: " + (.vector?|@html) + "\"/>\n<system-out>\n" +  (.packageName|@html) +" version: " + (.packageVersion|@html) + ", ref: "+ (.link|@html) + "\n</system-out>\n<system-err>\n" + (.description|@html) + "\n</system-err>\n</testcase>\n") | .[]'
      cat <<END
</testsuite>
END
		fi
	done

	cat <<END
</testsuites>
END
}

convert_tl_to_junit
