<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
    <g:set var="followmode" value="${params.mode in ['browse','tail','node']?params.mode:null==execution?.dateCompleted?'tail':'browse'}"/>
<g:set var="authKeys"
       value="${[AuthConstants.ACTION_KILL, AuthConstants.ACTION_READ, AuthConstants.ACTION_CREATE, AuthConstants.ACTION_RUN]}"/>
<g:set var="authChecks" value="${[:]}"/>
<g:each in="${authKeys}" var="actionName">
    <g:if test="${execution.scheduledExecution}">
    <%-- set auth values --%>
        %{
            authChecks[actionName] = auth.jobAllowedTest(job: execution.scheduledExecution, action: actionName)
        }%
    </g:if>
    <g:else>
        %{
            authChecks[actionName] = auth.adhocAllowedTest(action: actionName,project:execution.project)
        }%
    </g:else>
</g:each>
<g:set var="adhocRunAllowed" value="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN,project:execution.project)}"/>
<g:set var="jobCreateAllowed" value="${auth.resourceAllowedTest(kind: 'job', action: [AuthConstants.ACTION_CREATE],project:execution.project)}"/>



<g:render template="/execution/showFragment"
          model="[execution: execution, scheduledExecution: scheduledExecution, inlineView: true,authChecks:authChecks, jobCreateAllowed: jobCreateAllowed, adhocRunAllowed:adhocRunAllowed]"/>
