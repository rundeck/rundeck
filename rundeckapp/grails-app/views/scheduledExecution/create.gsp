<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - Create New <g:message code="domain.ScheduledExecution.title"/></title>
    <asset:javascript src="jobedit.js"/>
</head>
<body>


    <g:render template="/scheduledExecution/createForm" model="[scheduledExecution:scheduledExecution,crontab:crontab,iscopy:iscopy,authorized:authorized]"/>
</body>
</html>
