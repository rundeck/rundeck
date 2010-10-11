<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 12, 2008
  Time: 10:48:06 AM
  To change this template use File | Settings | File Templates.
--%>

<g:render template="/reports/menus" model="[menu:menu,submenu:submenu]"/>

<table border="0" cellpadding="0" cellspacing="0" class="subtoolbar floatl">
    <tr>
        <td>
            <span class="action subtool" onmousedown="menus.doMenuToggle(this,'rc_mainMenu');return false;"  style="padding:4px 2px;display:inline;">
                <img width="12px" height="12px" src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" class="disclosureicon"/>
                <g:if test="${!menu || 'events'==menu}">
                    <img src="${resource(dir:'images',file:'icon-med-Reportcenter.png')}" width="24px" height="24px" alt=""/>
                    Events
                </g:if>
            </span>
        </td>
        <td style="padding: 0 0 0 10px;" >
            <span class="action subtool" onmousedown="menus.doMenuToggle(this,'rc_${menu?menu:'events'}Menu');return false;"  style="padding:4px 2px;display:inline;">
                <img width="12px" height="12px" src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" class="disclosureicon"/>
                <g:if test="${!menu || 'events'==menu}">
                <g:if test="${!submenu || 'all'==submenu}">
                    <img src="${resource(dir:'images',file:'icon-med-events.png')}" width="24px" height="24px" alt=""/>
                    All Events
                </g:if>
                <g:elseif test="${'jobs'==submenu}">
                    <img src="${resource(dir:'images',file:'icon-small-job.png')}" width="16px" height="16px" alt=""/>
                    <g:message code="domain.ScheduledExecution.title"/>s
                </g:elseif>
                <g:elseif test="${'commands'==submenu}">
                    <img src="${resource(dir:'images',file:'icon-small-Command.png')}" width="16px" height="16px" alt=""/>
                    Commands
                </g:elseif>
                
                </g:if>
            </span>
        </td>
    </tr>
</table>