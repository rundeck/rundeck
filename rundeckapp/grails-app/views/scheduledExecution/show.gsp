<%@ page import="grails.util.Environment" %>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/> - <g:enc>${scheduledExecution?.jobName} : ${scheduledExecution?.description}</g:enc></title>
    <g:javascript library="prototype/effects"/>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <g:javascript library="executionOptions"/>
    <asset:javascript src="historyKO.js"/>
    <g:javascript>
        jQuery(document).ready(function(){
            var history = new History(appLinks.reportsEventsAjax, appLinks.menuNowrunningAjax);
            ko.applyBindings(history, document.getElementById('activity_section'));
            setupActivityLinks('activity_section', history);
        });
    </g:javascript>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


