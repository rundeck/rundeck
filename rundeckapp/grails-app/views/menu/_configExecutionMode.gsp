<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
    <div class="form-group">
        <label class=" control-label"><g:message code="executionMode.label"/>:</label>

        <g:ifExecutionMode>
            <p class=" form-control-static text-success"><g:message code="system.executionMode.status.active"/></p>

            <p class="help-block text-success">
                <g:message code="system.executionMode.description.active"/>
            </p>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <p class=" form-control-static text-warning"><g:message code="system.executionMode.status.passive"/></p>

            <p class="help-block text-warning">
                <g:message code="system.executionMode.description.passive"/>
            </p>
        </g:ifExecutionMode>
    </div>
<g:if test="${selected != 'changeexecmode'}">

    <g:set var="authAction" value="${g.executionMode(active:true)?AuthConstants.ACTION_DISABLE_EXECUTIONS:AuthConstants.ACTION_ENABLE_EXECUTIONS}"/>
    <auth:resourceAllowed action="${[authAction,AuthConstants.ACTION_ADMIN]}" any="true" context="application" kind="system">
        <g:ifExecutionMode active="true">
            <g:link action="executionMode" controller="menu" class="btn btn-default btn-sm">
                <g:message code="change.execution.mode" />
            </g:link>

        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <g:link action="executionMode" controller="menu" class="btn btn-default btn-sm">
                <g:message code="change.execution.mode" />
            </g:link>
        </g:ifExecutionMode>
    </auth:resourceAllowed>

</g:if>