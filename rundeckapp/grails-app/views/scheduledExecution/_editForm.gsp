<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<div class="pageTop obs_delete_show" style="display: none;">
    <span class="welcomeMessage">
        Delete <g:message code="domain.ScheduledExecution.title"/>
    </span>
</div>
<div class="pageBody ">
    <g:render template="/common/errorFragment"/>
    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}">
        <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
            <div id="${ukey}jobDisplayDeleteConf${scheduledExecution.id}" class="confirmBox popout obs_delete_show"
                 style="display:none; width: 300px; padding:20px; margin: 20px;">
                <g:form controller="scheduledExecution" action="delete" method="post">
                    <g:hiddenField name="id" value="${scheduledExecution.id}"/>
                    <g:render template="/scheduledExecution/showHead" model="${[scheduledExecution:scheduledExecution, runPage:true]}"/>
                    <div class="confirmMessage sepT">Really delete this <g:message
                            code="domain.ScheduledExecution.title"/>?</div>
                    <div class="buttons primary ">
                        <input type="submit" value="No" class="behavior_delete_hide"/>
                        <input type="submit" value="Yes"/>
                    </div>
                </g:form>
            </div>
        </g:if>
    </auth:resourceAllowed>
    <div class="obs_delete_hide" id="editForm">
    <g:form controller="scheduledExecution" method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="buttons primary obs_delete_hide" >

            <g:actionSubmit id="editFormCancelButton" value="Cancel"  onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
            <g:actionSubmit value="Save" action="Update"/>

        </div>

    </g:form>
    </div>
    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}">
        <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
            <div>
                <span class="action textbtn obs_delete_hide behavior_delete_show"
                      title="Delete ${g.message(code: 'domain.ScheduledExecution.title')}"
                      >
                    <img
                            src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="edit" width="12px"
                            height="12px"/> delete this Job</span>

            </div>
        </g:if>
    </auth:resourceAllowed>
</div>
<g:javascript>
fireWhenReady('editForm',function(){
    $$('.behavior_delete_show').each(function(e){
        Event.observe(e,'click',function(evt){
            evt.stop();
            $$('.obs_delete_hide').each(Element.hide);
            $$('.obs_delete_show').each(Element.show);
        })
    });
    $$('.behavior_delete_hide').each(function(e){
        Event.observe(e,'click',function(evt){
            evt.stop();
            $$('.obs_delete_hide').each(Element.show);
            $$('.obs_delete_show').each(Element.hide);
        })
    });
});
</g:javascript>
