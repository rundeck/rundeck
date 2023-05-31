#!/bin/bash
set -euo pipefail
readonly ARGS=("$@")
IN=${ARGS[0]:-/dev/stdin}
THRESHOLD=${THRESHOLD:-2}


#/ use jq to convert twistlock scan into a CSV
convert_tl_to_csv(){
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
Severity;Package;Version;CVE;Status;Ref;Path;Description
END

	for sev in "${sevs[@]:$THRESHOLD}" ; do
		local count=$(jq -r ".results[0].vulnerabilityDistribution.$sev" < "$IN")
		if [[ $count -gt 0 ]]; then

      jq -a ".results[0].vulnerabilities | map(select(.severity == \"$sev\"))" < "$IN"  |
        jq -r "map(\"$sev\" + \";\" + (.packageName | @text) + \";\" + (.packageVersion|@text) + \";\" + (.id | @text) + \";\" + (.status | @text) + \";\" + (.link|@text) + \";\" + (.packagePath | @text) + \";\" + (.description|@html)) | .[]"
		fi
	done


}

convert_tl_to_csv
