<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:form class=" form-inline" controller="execution" action="executionMode" method="POST" useToken="true">
    <g:hiddenField name="mode" value="${g.executionMode(active: true)?'passive':'active'}"/>
    <g:hiddenField name="project" value="${params.project}"/>
    <div class="form-group">
        <label class=" control-label">Executions:</label>

        <g:ifExecutionMode>
            <p class=" form-control-static text-info">ACTIVE</p>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <p class=" form-control-static text-warning">PASSIVE</p>
        </g:ifExecutionMode>
    </div>

    <auth:resourceAllowed action="${AuthConstants.ACTION_TOGGLE_ACTIVE}"  context="application" kind="system">
        <g:ifExecutionMode active="true">
            <button type="submit"
                    class="btn btn-warning "
                    title="Toggle execution mode"
            >
                <i class="glyphicon glyphicon-ban-circle"></i>
                Set Passive Mode
            </button>
        </g:ifExecutionMode>
        <g:ifExecutionMode passive="true">
            <button type="submit"
                    class="btn btn-success "
                    title="Toggle execution mode"
            >
                <i class="glyphicon glyphicon-play-circle"></i>
                Set Active Mode
            </button>
        </g:ifExecutionMode>
        <p class="help-block">
            Toggle the Execution Mode for this server.
        </p>
    </auth:resourceAllowed>
</g:form>