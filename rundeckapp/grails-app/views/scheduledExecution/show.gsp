<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base" />
    <meta name="tabpage" content="jobs"/>
    <title><g:message code="main.app.name"/> - ${scheduledExecution?.jobName.encodeAsHTML()} : ${scheduledExecution?.description?.encodeAsHTML()}</title>
    <g:javascript library="prototype/effects"/>
  </head>

  <body>
        <tmpl:show scheduledExecution="${scheduledExecution}"  crontab="${crontab}"/>
  </body>
</html>


