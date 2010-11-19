<div class="pageTop extra">
<div class="jobHead">
    <tmpl:showHead scheduledExecution="${scheduledExecution}" iconName="icon-job" subtitle="Choose Execution Options"/>
    <div class="clear"></div>
    
    <g:if test="${scheduledExecution?.extraInfo}">
    <div class="schedExecPagePart">
        <div class="presentation"  id="schedExExtra${scheduledExecution?.id}">
            <g:if test="${scheduledExecution?.extraInfo}">
            <div class="extraInfo">
                ${scheduledExecution?.extraInfo?.encodeAsHTML()}
            </div>
            </g:if>
            <g:else>
                <div class="note empty">None</div>
            </g:else>

        </div>
    </div>
    </g:if>

</div>
<div class="pageSubtitle subtitleAction">
        Choose Execution Options
</div>
    <div class="clear"></div>
</div>
<div class="pageBody form">
    <g:form controller="scheduledExecution" method="post">
        <g:render template="editOptions" model="${[scheduledExecution:scheduledExecution, options:options, command:command, selectedoptsmap:selectedoptsmap, selectedargstring:selectedargstring,authorized:authorized]}"/>

        <g:if test="${nodesetempty }">
            <div class="error note">
                <g:message code="scheduledExecution.nodeset.empty.warning"/>
            </div>
        </g:if>
        <div class="buttons" id="formbuttons">

            <g:actionSubmit id="execFormCancelButton" value="Cancel"/>
            <g:actionSubmit value="Run ${g.message(code:'domain.ScheduledExecution.title')} Now"/>

        </div>
        <div class="error note" id="formerror" style="display:none">

        </div>
    </g:form>
</div>