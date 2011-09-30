<div class="pageTop extra">
<div class="jobHead">
    <tmpl:showHead scheduledExecution="${scheduledExecution}" iconName="icon-job" subtitle="Choose Execution Options"/>
    <div class="clear"></div>
    
</div>
<div class="pageSubtitle subtitleAction">
        Choose Execution Options
</div>
    <div class="clear"></div>
</div>
<div class="pageBody form">
    <g:form controller="scheduledExecution" method="post">
        <g:render template="editOptions" model="${[scheduledExecution:scheduledExecution, selectedoptsmap:selectedoptsmap, selectedargstring:selectedargstring,authorized:authorized,jobexecOptionErrors:jobexecOptionErrors]}"/>
        <input name="extra._replaceNodeFilters" value="true" type="hidden"/>

        <g:if test="${nodesetvariables }">
            <div class="message note">
                <g:message code="scheduledExecution.nodeset.variable.warning" default="Note: The Node filters specified for this Job contain variable references, and the runtime nodeset cannot be determined."/>
            </div>
        </g:if>
        <g:elseif test="${nodesetempty }">
            <div class="error note">
                <g:message code="scheduledExecution.nodeset.empty.warning"/>
            </div>
        </g:elseif>
        <g:elseif test="${nodes}">
            <span class="prompt">Nodes:</span>
            <div class="presentation">
                <table>
                <g:each var="node" in="${nodes}" status="index">
                    <g:if test="${index % 10 == 0}">
                        <tr>
                    </g:if>
                    <td>
                        <input id="${node.nodename}" type="checkbox" name="extra.nodeIncludeName" value="${node.nodename}" checked="true"/>
                        <label for="${node.nodename}">${node.nodename}</label>
                    </td>
                    <g:if test="${(index % 10 == 9) || (index+1 == nodes.size())}">
                        </tr>
                    </g:if>
                </g:each>
                </table>
            </div>
        </g:elseif>
        <div class="buttons" id="formbuttons">

            <g:actionSubmit id="execFormCancelButton" value="Cancel"/>
            <g:actionSubmit value="Run ${g.message(code:'domain.ScheduledExecution.title')} Now" id="execFormRunButton"/>

        </div>
        <div class="error note" id="formerror" style="display:none">

        </div>
    </g:form>
</div>