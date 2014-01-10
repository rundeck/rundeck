<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>

<g:render template="/common/errorFragment"/>
<auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}" project="${scheduledExecution.project?: params.project ?: request.project}">
    <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE,project:scheduledExecution.project)}">
        <div class="modal" id="jobdelete" tabindex="-1" role="dialog" aria-labelledby="deletejobtitle" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="deletejobtitle">Delete <g:message code="domain.ScheduledExecution.title"/></h4>
                    </div>

                    <div class="modal-body">
                        <g:render template="/scheduledExecution/showHead"
                                  model="${[scheduledExecution: scheduledExecution, runPage: true]}"/>
                        <p class=" ">Really delete this <g:message
                                code="domain.ScheduledExecution.title"/>?</p>
                    </div>

                    <div class="modal-footer">
                        <g:form controller="scheduledExecution" action="delete" method="post">
                            <g:hiddenField name="id" value="${scheduledExecution.extid}"/>
                            <button type="submit" class="btn btn-default btn-sm "
                                    data-dismiss="modal">
                                Cancel
                            </button>
                            <input type="submit" value="Delete" class="btn btn-danger btn-sm"/>
                        </g:form>
                    </div>
                </div>
            </div>
        </div>
    </g:if>
</auth:resourceAllowed>
<g:form controller="scheduledExecution" method="post"
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
                                class="btn btn-default"/>
                <g:actionSubmit value="Save" action="Update" class="btn btn-primary "/>

            </div>
            <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}" project="${scheduledExecution.project}">
                <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
                    <div class="  col-sm-2">

                        <a data-toggle="modal"
                           href="#jobdelete"
                                class="textbtn textbtn-danger pull-right"
                              title="Delete ${g.message(code: 'domain.ScheduledExecution.title')}">
                            <b class="glyphicon glyphicon-remove-circle"></b>
                            Delete this Job
                        </a>

                    </div>
                </g:if>
            </auth:resourceAllowed>
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
