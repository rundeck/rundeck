#!/usr/bin/env bash
# [RUN-4839] Run the CodeNarc cyclomatic complexity check locally (informative only).
# Threshold and rules: config/codenarc/rules.groovy. Production code only —
# tests/specs are excluded.
# Frontend equivalent: `npm run lint` in rundeckapp/grails-spa/packages/ui-trellis
# (the `complexity` ESLint rule is enabled in its .eslintrc.js).
#
# Usage: scripts/codenarc-complexity.sh
# Reports: build/reports/codenarc.html (+ text summary on stdout)
set -euo pipefail

CODENARC_VERSION=3.6.0-groovy-4.0
GMETRICS_VERSION=2.1.0
GROOVY_VERSION=4.0.33
SLF4J_VERSION=1.7.36

CACHE="${HOME}/.cache/codenarc-jars"
mkdir -p "${CACHE}"

JAR_URLS=(
  "https://repo1.maven.org/maven2/org/codenarc/CodeNarc/${CODENARC_VERSION}/CodeNarc-${CODENARC_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/gmetrics/GMetrics-Groovy4/${GMETRICS_VERSION}/GMetrics-Groovy4-${GMETRICS_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/apache/groovy/groovy/${GROOVY_VERSION}/groovy-${GROOVY_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/apache/groovy/groovy-xml/${GROOVY_VERSION}/groovy-xml-${GROOVY_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/apache/groovy/groovy-templates/${GROOVY_VERSION}/groovy-templates-${GROOVY_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/apache/groovy/groovy-ant/${GROOVY_VERSION}/groovy-ant-${GROOVY_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/${SLF4J_VERSION}/slf4j-api-${SLF4J_VERSION}.jar"
  "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/${SLF4J_VERSION}/slf4j-simple-${SLF4J_VERSION}.jar"
)
for url in "${JAR_URLS[@]}"; do
  jar="${CACHE}/$(basename "${url}")"
  [ -f "${jar}" ] || curl -sf -o "${jar}" "${url}"
done

cd "$(dirname "$0")/.."
mkdir -p build/reports

java -Xmx2g -cp "${CACHE}/*" org.codenarc.CodeNarc \
  -basedir=. \
  -includes='**/*.groovy' \
  -excludes='**/build/**,**/node_modules/**,**/.gradle/**,**/src/test/**,functional-test/**,test/**,**/*Spec.groovy,**/*Test.groovy' \
  -rulesetfiles=file:config/codenarc/rules.groovy \
  -report=html:build/reports/codenarc.html \
  -report=text:stdout

echo "HTML report: build/reports/codenarc.html"
