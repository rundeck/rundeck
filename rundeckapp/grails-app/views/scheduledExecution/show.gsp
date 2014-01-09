<%@ page import="grails.util.Environment" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/> - ${scheduledExecution?.jobName.encodeAsHTML()} : ${scheduledExecution?.description?.encodeAsHTML()}</title>
    <g:javascript library="prototype/effects"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <g:javascript library="executionOptions"/>
    <g:if test="${grails.util.Environment.current == Environment.DEVELOPMENT}">
        <g:javascript src="knockout-3.0.0.debug.js"/>
    </g:if>
    <g:else>
        <g:javascript src="knockout-3.0.0-min.js"/>
    </g:else>
    <g:javascript src="knockout.mapping-latest.js"/>
    <g:javascript src="moment.min.js"/>
    <asset:javascript src="momentutil.js"/>
    <g:javascript src="historyKO.js"/>
    <g:javascript>
        jQuery(document).ready(function(){
            var history = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
            ko.applyBindings(history, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', history, appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
        });
    </g:javascript>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


