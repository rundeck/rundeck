<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:form class="form-inline" controller="execution" action="executionMode" method="POST" useToken="true">
    <g:hiddenField name="mode" value="${g.executionMode(active: true) ? 'passive' : 'active'}"/>
    <g:hiddenField name="project" value="${params.project}"/>
    <div class="form-group">
        <label class=" control-label"><g:message code="executionMode.label"/>:</label>

        <g:ifExecutionMode>
            <p class=" form-control-static text-info"><g:message code="system.executionMode.status.active"/></p>

            <p class="help-block text-info">
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

    <auth:resourceAllowed action="${AuthConstants.ACTION_TOGGLE_ACTIVE}" context="application" kind="system">
        <g:ifExecutionMode active="true">
            <button type="submit"
                    class="btn btn-warning "
                    title="Toggle execution mode">
                <i class="glyphicon glyphicon-ban-circle"></i>
                <g:message code="action.executionMode.set.passive.label"/>
            </button>

            <p class="help-block">
                <g:message code="action.executionMode.set.passive.help"/>
            </p>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <button type="submit"
                    class="btn btn-success "
                    title="Toggle execution mode">
                <i class="glyphicon glyphicon-play-circle"></i>
                <g:message code="action.executionMode.set.active.label"/>
            </button>

            <p class="help-block">
                <g:message code="action.executionMode.set.active.help"/>
            </p>
        </g:ifExecutionMode>
    </auth:resourceAllowed>
</g:form>