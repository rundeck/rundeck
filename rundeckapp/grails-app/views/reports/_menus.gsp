<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 12, 2008
  Time: 10:46:24 AM
  To change this template use File | Settings | File Templates.
--%>

<!--tool menu -->
<div id="rc_mainMenu" class="summarySubtoolMenu" style="display:none;line-height:18px;">
    <div class="summarySubmenu">
        <div class="menuSection">
            <g:link class="compbutton" style="display:block;padding:5px;border:0;" controller="reports" action="index"   params="${paginateParams?paginateParams:[:]}">
                <img src="${resource(dir:'images',file:'icon-med-Reportcenter.png')}" width="24px" height="24px" alt=""/>
                Events
            </g:link>
        </div>
    </div>
</div>

<!--submenu for Events-->
<div id="rc_eventsMenu" class="summarySubtoolMenu" style="display:none;line-height:18px;">
    <div class="summarySubmenu">
        <div class="menuSection">
            <g:link class="compbutton" style="display:block;padding:5px;border:0;" action="index"  controller="reports"  params="${paginateParams?paginateParams:[:]}">
                <img src="${resource(dir:'images',file:'icon-med-events.png')}" width="24px" height="24px" alt=""/>
                All Events
            </g:link>
        <ul>
            <li>
                <g:link class="subtool"  action="jobs" controller="reports" params="${paginateParams?paginateParams:[:]}">
                <img src="${resource(dir:'images',file:'icon-small-job.png')}" width="16px" height="16px" alt=""/>
                    <g:message code="domain.ScheduledExecution.title"/>s</g:link>
            </li>
            <li>
                <g:link class="subtool"  action="commands"  controller="reports" params="${paginateParams?paginateParams:[:]}">
                    <img src="${resource(dir:'images',file:'icon-small-Command.png')}" width="16px" height="16px" alt=""/>
                    Commands</g:link>
            </li>
            </ul>
        </div>
    </div>
</div>


<script type="text/javascript">
//<!--
Event.observe(window,'load', function(e){
    Event.observe($('rc_mainMenu'),'mouseout',function(e){menus._mouseoutMenuHide(e,'rc_mainMenu');});
    Event.observe($('rc_eventsMenu'),'mouseout',function(e){menus._mouseoutMenuHide(e,'rc_eventsMenu');});
    Event.observe($('rc_mainMenu'),'mouseover',function(e){menus._mouseoverMenuRestore(e,'rc_mainMenu');});
    Event.observe($('rc_eventsMenu'),'mouseover',function(e){menus._mouseoverMenuRestore(e,'rc_eventsMenu');});
});
//-->
</script>