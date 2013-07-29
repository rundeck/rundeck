<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants; rundeck.Execution" %>
<g:javascript>
/** START history
         *
         */
        function loadHistory(){
            new Ajax.Updater('histcontent',"${createLink(controller: 'reports', action: 'eventsFragment')}",{
                parameters:{compact:true,nofilters:true,jobIdFilter:'${scheduledExecution.id}'},
                evalScripts:true,
                onComplete: function(transport) {
                    if (transport.request.success()) {
                        Element.show('histcontent');
                    }
                }
            });
        }

    function init(){
        <g:if test="${!(grailsApplication.config.rundeck?.gui?.enableJobHoverInfo in ['false', false])}">
        $$('.obs_bubblepopup').each(function(e) {
            new BubbleController(e,null,{offx:-14,offy:null}).startObserving();
        });
        </g:if>
    }
    Event.observe(window,'load',init);

</g:javascript>

<div class="pageTop extra">
    <div class="jobHead">
        <g:render template="/scheduledExecution/showHead" model="[scheduledExecution:scheduledExecution,followparams:[mode:followmode,lastlines:params.lastlines]]"/>
    </div>

    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_RUN)}">
    <tmpl:execOptionsForm model="${[scheduledExecution: scheduledExecution, crontab: crontab, authorized: authorized]}"
                          hideHead="${true}"
        hideCancel="${true}"
    />
    </g:if>
    <div class="clear"></div>
</div>

<div class="pageBody" id="schedExecPage">
    <g:expander key="schedExDetails${scheduledExecution?.id}">Definition </g:expander>
    <div id="schedExDetails${scheduledExecution?.id}" style="display: none">
        <g:render template="showDetail" model="[scheduledExecution:scheduledExecution,showEdit:true,hideOptions:true]"/>

    </div>

    <div class="pageMessage" id="showPageMessage" style="display: none;"></div>

</div>
<div class="runbox"><g:message code="page.section.Activity"/></div>
<div class="pageBody">
    <g:render template="/scheduledExecution/renderJobStats" model="${[scheduledExecution: scheduledExecution]}"/>

    <table cellpadding="0" cellspacing="0" class="jobsList list history" style="width:100%">
        <tbody id="nowrunning"></tbody>
        <tbody id="histcontent"></tbody>
    </table>
    <g:javascript>
        fireWhenReady('histcontent', loadHistory);
    </g:javascript>
</div>

<g:javascript library="ace/ace"/>
<g:javascript>
    fireWhenReady('schedExecPage', function (z) {
        $$('.apply_ace').each(function (t) {
            _applyAce(t,'400px');
        })
    });
</g:javascript>
