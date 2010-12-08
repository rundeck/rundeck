<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - Edit <g:message code="domain.ScheduledExecution.title"/></title>
</head>
<body>

    <div class="pageTop">
        <span class="welcomeMessage">
        Edit <g:message code="domain.ScheduledExecution.title"/>
        </span>
    </div>
    <tmpl:editForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized]"/>
</body>
</html>
