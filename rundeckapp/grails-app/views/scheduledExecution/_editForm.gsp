<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<div class="pageBody ">
    <g:render template="/common/errorFragment"/>
    <g:form controller="scheduledExecution" method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="buttons primary">

            <g:actionSubmit id="editFormCancelButton" value="Cancel"  onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
            <g:actionSubmit value="Save" action="Update"/>

        </div>

        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE}">
            <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_DELETE)}">
                <div class="buttons">
                <span class="action textbtn" title="Delete ${g.message(code: 'domain.ScheduledExecution.title')}"
                      onclick="menus.showRelativeTo(this, '${ukey}jobDisplayDeleteConf${scheduledExecution.id}', -2, -2);
                      return false;">
                    <img
                            src="${resource(dir: 'images', file: 'icon-tiny-removex.png')}" alt="edit" width="12px"
                            height="12px"/> delete this Job</span>

                <div id="${ukey}jobDisplayDeleteConf${scheduledExecution.id}" class="confirmBox popout"
                     style="display:none;">
                    <g:form controller="scheduledExecution" action="delete" method="post"
                            id="${scheduledExecution.extid}">
                        <span class="confirmMessage">Really delete this <g:message
                                code="domain.ScheduledExecution.title"/>?</span>
                        <input type="submit" value="No"
                               onclick="Element.toggle('${ukey}jobDisplayDeleteConf${scheduledExecution.id}');
                               return false;"/>
                        <input type="submit" value="Yes"/>
                    </g:form>
                </div>
                </div>
            </g:if>
        </auth:resourceAllowed>
    </g:form>
</div>
