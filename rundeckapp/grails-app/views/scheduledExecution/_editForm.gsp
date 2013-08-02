<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<div class="pageBody ">
    <g:render template="/common/errorFragment"/>
    <g:form controller="scheduledExecution" method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="buttons primary obs_delete_hide" >

            <g:actionSubmit id="editFormCancelButton" value="Cancel"  onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
            <g:actionSubmit value="Save" action="Update"/>

        </div>

    </g:form>
    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}">
        <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
            <div >
                <span class="action textbtn obs_delete_hide" title="Delete ${g.message(code: 'domain.ScheduledExecution.title')}"
                      onclick="menus.showRelativeTo(this, '${ukey}jobDisplayDeleteConf${scheduledExecution.id}', -2, -2);
                      $$('.obs_delete_hide').each(Element.hide);
                      return false;">
                    <img
                            src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="edit" width="12px"
                            height="12px"/> delete this Job</span>

                <div id="${ukey}jobDisplayDeleteConf${scheduledExecution.id}" class="confirmBox popout"
                     style="display:none; width: 300px;">
                    <g:form controller="scheduledExecution" action="delete" method="post">
                        <g:hiddenField name="id" value="${scheduledExecution.id}"/>
                        <span class="confirmMessage">Really delete this <g:message
                                code="domain.ScheduledExecution.title"/>?</span>
                        <input type="submit" value="No"
                               onclick="Element.toggle('${ukey}jobDisplayDeleteConf${scheduledExecution.id}');
                               $$('.obs_delete_hide').each(Element.show);
                               return false;"/>
                        <input type="submit" value="Yes"/>
                    </g:form>
                </div>
            </div>
        </g:if>
    </auth:resourceAllowed>
</div>
