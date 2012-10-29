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
        <g:render template="/scheduledExecution/showHead" model="[scheduledExecution:scheduledExecution,execution:execution,followparams:[mode:followmode,lastlines:params.lastlines]]"/>
    </div>

    <div style="vertical-align:top;width: 200px;" class="toolbar small">
        <g:render template="/scheduledExecution/actionButtons" model="${[scheduledExecution:scheduledExecution,objexists:objexists,jobAuthorized:jobAuthorized]}"/>
        <g:set var="lastrun" value="${scheduledExecution.id?Execution.findByScheduledExecutionAndDateCompletedIsNotNull(scheduledExecution,[max: 1, sort:'dateStarted', order:'desc']):null}"/>
        <g:set var="successcount" value="${scheduledExecution.id?Execution.countByScheduledExecutionAndStatus(scheduledExecution,'true'):0}"/>
        <g:set var="execCount" value="${scheduledExecution.id?Execution.countByScheduledExecution(scheduledExecution):0}"/>
        <g:set var="successrate" value="${execCount>0? (successcount/execCount) : 0}"/>
        <g:render template="/scheduledExecution/showStats" model="[scheduledExecution:scheduledExecution,lastrun:lastrun?lastrun:null, successrate:successrate]"/>
    </div>
    <div class="clear"></div>
</div>

<div class="pageBody" id="schedExecPage">

    %{--<g:expander key="schedExDetails${scheduledExecution?.id?scheduledExecution?.id:''}" imgfirst="true">Details</g:expander>--}%
    <span class="prompt">Details</span>
    <div class="presentation"  id="schedExDetails${scheduledExecution?.id}" style="max-width:600px; width:600px;">
        <g:render template="showDetail" model="[scheduledExecution:scheduledExecution]"/>

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
<div class="runbox">History</div>
<div class="pageBody">
    <div id="histcontent"></div>
    <g:javascript>
        fireWhenReady('histcontent', loadHistory);
    </g:javascript>
</div>
