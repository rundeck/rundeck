#!/bin/bash

#test metrics API endpoints

DIR=$(cd `dirname $0` && pwd)
export API_CURRENT_VERSION=${API_VERSION}
source $DIR/include.sh

file=$DIR/curl.out

test_metrics_list(){

    ENDPOINT="${APIURL}/metrics"
    ACCEPT="application/json"

    test_begin "List Metrics Links (json)"

    api_request "$ENDPOINT" "${file}"

    assert_json_value '4' '._links | length'  "${file}"
    assert_json_value "${APIURL}/metrics/metrics"  '._links.metrics.href' "${file}"
    assert_json_value "${APIURL}/metrics/ping"  '._links.ping.href' "${file}"
    assert_json_value "${APIURL}/metrics/healthcheck"  '._links.healthcheck.href' "${file}"
    assert_json_value "${APIURL}/metrics/threads"  '._links.threads.href' "${file}"
    
    test_succeed

    rm "${file}"
}
test_metrics_metrics(){

    ENDPOINT="${APIURL}/metrics/metrics"
    ACCEPT="application/json"

    test_begin "Get Metrics Data (json)"

    api_request "$ENDPOINT" "${file}"

    local VAL=$(jq -r '.gauges | length'  "${file}")
    [ "$VAL" -gt 0 ] || fail "Expected > 0 for .gauges in $file but was $VAL"
    VAL=$(jq -r '.counters | length'  "${file}" )
    [ "$VAL" -gt 0 ] || fail "Expected > 0 for .counters in $file but was $VAL"
    assert_json_value '8' '.meters | length'  "${file}"
    assert_json_value '17' '.timers | keys | map(select(. == ["org.rundeck.app.authorization.TimedAuthContextEvaluator.authorizeProjectJobAll","org.rundeck.app.authorization.TimedAuthContextEvaluator.authorizeProjectResource","org.rundeck.app.authorization.TimedAuthContextEvaluator.authorizeProjectResourceAny","org.rundeck.app.authorization.TimedAuthContextEvaluator.authorizeProjectResources","org.rundeck.app.authorization.TimedAuthContextEvaluator.filterAuthorizedProjectExecutionsAll","rundeck.api.requests.requestTimer","rundeck.controllers.MenuController.apiExecutionsRunningv14.queryQueue","rundeck.controllers.ReportsController.apiHistory.getExecutionReports","rundeck.quartzjobs.ExecutionJob.executionTimer","rundeck.services.AuthorizationService.getSystemAuthorization","rundeck.services.AuthorizationService.systemAuthorization.evaluateSetTimer","rundeck.services.AuthorizationService.systemAuthorization.evaluateTimer","rundeck.services.ExecutionService.runJobReference","rundeck.services.FrameworkService.filterNodeSet","rundeck.services.NodeService.project.APIImportAndCleanHistoryTest.loadNodes","rundeck.services.NodeService.project.test.loadNodes","rundeck.web.requests.requestTimer"][])) | length'  "${file}"

    test_succeed

    rm "${file}"
}
test_metrics_healthcheck(){

    ENDPOINT="${APIURL}/metrics/healthcheck"
    ACCEPT="application/json"

    test_begin "Get Metrics healthcheck (json)"

    api_request "$ENDPOINT" "${file}"

    assert_json_value '2' 'length'  "${file}"
    assert_json_value 'true' '.["dataSource.connection.time"].healthy'  "${file}"
    assert_json_value 'true' '.["quartz.scheduler.threadPool"].healthy'  "${file}"
    
    test_succeed

    rm "${file}"
}
test_metrics_ping(){

    ENDPOINT="${APIURL}/metrics/ping"
    ACCEPT="text/plain"

    test_begin "Get Metrics ping (text)"

    api_request "$ENDPOINT" "${file}"

    local VAL=$(head -n 1 "${file}")
    assert "pong" "$VAL"
    local LEN=$(wc -l "$file"| awk \{'print $1'\} )
    assert "1" "$LEN"
    
    test_succeed

    rm "${file}"
}
test_metrics_threads(){

    ENDPOINT="${APIURL}/metrics/threads"
    ACCEPT="text/plain"

    test_begin "Get Metrics threads (text)"

    api_request "$ENDPOINT" "${file}"

    local VAL=$(wc -l "$file"| awk \{'print $1'\} )
    
    if [ "$VAL" -lt 10 ] ; then
        fail "Expected threads output to have > 10 lines"
    fi
    
    test_succeed

    rm "${file}"
}
main(){
    test_metrics_list
    test_metrics_metrics
    test_metrics_healthcheck
    test_metrics_ping
    test_metrics_threads
}
main
