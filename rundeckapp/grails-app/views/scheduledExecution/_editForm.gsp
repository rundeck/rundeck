<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:render template="/common/errorFragment"/>

%{--Edit job form--}%
<g:form controller="scheduledExecution" method="post"
        action="update"
        params="[project:params.project]"
        useToken="true"
        class="form-horizontal"
        onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">

<div class="panel panel-primary obs_delete_hide" id="editForm">
    <div class="panel-heading">
        <span class="h4">
            Edit <g:message code="domain.ScheduledExecution.title"/>
        </span>
    </div>

        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="panel-footer">
            <div class="row">
            <div class="buttons col-sm-10">

                <g:actionSubmit id="editFormCancelButton" value="Cancel"
                                onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"
                                class="btn btn-default reset_page_confirm"/>
                <g:actionSubmit value="Save" action="Update" class="btn btn-primary reset_page_confirm "/>

            </div>
            </div>
        </div>

</div>

</g:form>
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
