<table class="userauth ">
    <g:if test="${showAdmin}">
        <g:ifUserInAnyRoles roles="admin,user_admin">
            <tr>
                <td class="section admin" colspan="3" >
                    <table cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                    <td >administrator</td>
                    <td style="width:12px;">
                        <tmpl:authItem  value="${true}"/>
                    </td>
                    </tr>
                    </table>
                </td>
            </tr>
        </g:ifUserInAnyRoles>
    </g:if>
    <tr>
        <td class="section">
            <table cellpadding="0" cellspacing="0">
                <tr>
                    <th colspan="2"><g:message code="domain.ScheduledExecution.title"/>s</th>
                </tr>
                <g:each var="access" in="${['create','read','update','delete','run','kill']}">
                    <tr class="${user.authorization?.('workflow_'+access)?'allowed':'disallowed'}">
                        <td class="accesslabel">${access}</td>
                        <td class="access">
                            <tmpl:authItem  user="${user}" key="workflow_${access}"/>
                        </td>
                    </tr>
                </g:each>
            </table>
        </td>
        <td class="section">

            <table cellpadding="0" cellspacing="0">
                <tr>
                    <th colspan="2">Events</th>
                </tr>
                <g:each var="access" in="${['create','read','update','delete']}">
                    <tr class="${user.authorization?.('events_'+access)?'allowed':'disallowed'}">
                        <td class="accesslabel">${access}</td>
                        <td class="access">
                            <tmpl:authItem  user="${user}" key="events_${access}"/>
                        </td>
                    </tr>
                </g:each>
            </table>
        </td>
        <td class="section">

            <table cellpadding="0" cellspacing="0">
                <tr>
                    <th colspan="2">Resources</th>
                </tr>
                <g:each var="access" in="${['create','read','update','delete']}">
                    <tr class="${user.authorization?.('resources_'+access)?'allowed':'disallowed'}">
                        <td class="accesslabel">${access}</td>
                        <td class="access">
                            <tmpl:authItem  user="${user}" key="resources_${access}"/>
                        </td>
                    </tr>
                </g:each>
            </table>

        </td>
    </tr>
</table>