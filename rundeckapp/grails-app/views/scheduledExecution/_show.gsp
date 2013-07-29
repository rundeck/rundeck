<%@ page import="rundeck.Execution" %>
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

    <div class="clear"></div>
</div>

<div class="pageBody" id="schedExecPage">
    <div id="schedExDetails${scheduledExecution?.id}" style="">
        <g:render template="showDetail" model="[scheduledExecution:scheduledExecution,showEdit:true]"/>

    </div>

    <g:if test="${flash.message}">
        <div class="message">${flash.message}</div>
    </g:if>
    <g:if test="${message}">
        <div class="message">${message}</div>
    </g:if>
    <div class="pageMessage" id="showPageMessage" style="display: none;"></div>
    <g:render template="/common/messages"/>

</div>
<div class="runbox"><g:message code="page.section.Activity"/></div>
<div class="pageBody">
    <g:render template="/scheduledExecution/renderJobStats" model="${[scheduledExecution: scheduledExecution]}"/>

    <table cellpadding="0" cellspacing="0" class="jobsList list history" style="width:100%">
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
