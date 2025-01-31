package org.rundeck.tests.functional.api.authorizations

import org.rundeck.util.annotations.APITest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.BaseContainer

@APITest
class AuthorizationsSpec extends BaseContainer {

    static final def projectName = UUID.randomUUID().toString()
    static final def action1 = 'admin'
    static final def action2 = 'read'
    static final def ACTIONS_QUERY_PARAMS = "actions=$action1&actions=$action2"

    static def jobId

    def setupSpec() {
        setupProject(projectName)

        def jobName1 = "scheduledJob1"
        def jobXml1 = JobUtils.generateScheduledJobsXml(jobName1)

        def job1CreatedParsedResponse = JobUtils.createJob(projectName, jobXml1, client)
        jobId = job1CreatedParsedResponse.succeeded[0]?.id
    }

    def "verify authorizations/application/{kind} successful response"() {
        given:
        def kind = 'system_acl'
        when:
            def data = doGet("/authorizations/application/$kind?$ACTIONS_QUERY_PARAMS")
        then:
            data.code() == 200
            def jsonResponse = getClient().jsonValue(data.body(), Map)
            jsonResponse.authorizationContext.type == 'application'
            jsonResponse.resource.kind == kind
            jsonResponse.actionAuthorizations.size() == 2
            jsonResponse.actionAuthorizations.findAll {it.isAuthorized }.size() == 2
            jsonResponse.actionAuthorizations.collect { it.actionName }.containsAll([action1, action2])
    }

    def "verify authorizations/application/{type}/{specifier} successful response"() {
        given:
        final def type = 'project_acl'
        final def specifier = 'proj1'
        when:
        def data = doGet("/authorizations/application/$type/$specifier?$ACTIONS_QUERY_PARAMS")
        then:
        data.code() == 200
        def jsonResponse = getClient().jsonValue(data.body(), Map)
        jsonResponse.authorizationContext.type == 'application'
        jsonResponse.resource.type == type
        jsonResponse.resource.specifier == specifier
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized }.size() == 2
        jsonResponse.actionAuthorizations.collect { it.actionName }.containsAll([action1, action2])
    }

    def "verify authorizations/projects/{project}/{kind} successful response"() {
        given:
        final def kind = 'job'
        when:
        def data = doGet("/authorizations/project/$projectName/$kind?$ACTIONS_QUERY_PARAMS")
        then:
        data.code() == 200
        def jsonResponse = getClient().jsonValue(data.body(), Map)
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.kind == kind
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized }.size() == 2
        jsonResponse.actionAuthorizations.collect { it.actionName }.containsAll([action1, action2])
    }

    def "verify authorizations/project/{project}/{type}/{specifier} successful response"() {
        given:
        final def type = 'node'
        final def specifier = 'node123'
        when:
        def data = doGet("/authorizations/project/$projectName/$type/$specifier?$ACTIONS_QUERY_PARAMS")
        then:
        data.code() == 200
        def jsonResponse = getClient().jsonValue(data.body(), Map)
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.type == type
        jsonResponse.resource.specifier == specifier
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized }.size() == 2
        jsonResponse.actionAuthorizations.collect { it.actionName }.containsAll([action1, action2])
    }

    def "verify authorizations/project/{project}/job/{specifier} successful response"() {
        given:
        final def jobName = 'Name of job123'
        when:
        def data = doGet("/authorizations/project/$projectName/job/$jobId?$ACTIONS_QUERY_PARAMS")
        then:
        data.code() == 200
        def jsonResponse = getClient().jsonValue(data.body(), Map)
        jsonResponse.authorizationContext.type == 'project'
        jsonResponse.authorizationContext.name == projectName
        jsonResponse.resource.type == 'job'
        jsonResponse.resource.specifier == jobId
        jsonResponse.actionAuthorizations.size() == 2
        jsonResponse.actionAuthorizations.findAll {it.isAuthorized }.size() == 2
        jsonResponse.actionAuthorizations.collect { it.actionName }.containsAll([action1, action2])
    }
}
