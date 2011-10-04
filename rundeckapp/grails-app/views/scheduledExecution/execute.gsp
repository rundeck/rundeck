<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base" />
    <title><g:message code="main.app.name"/> - ${scheduledExecution?.jobName.encodeAsHTML()} : ${scheduledExecution?.description?.encodeAsHTML()} : Execute <g:message code="domain.ScheduledExecution.title"/>  &hellip;</title>
    <g:render template="/framework/remoteOptionValuesJS"/>
    <g:javascript library="prototype/effects"/>
    <g:javascript library="executionOptions"/>
  </head>

  <body>

<div class="pageBody solo frame">
    <tmpl:execOptionsForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized]"/>
</div>
  </body>
</html>


