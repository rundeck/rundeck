<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - Create New <g:message code="domain.ScheduledExecution.title"/></title>
</head>
<body>

    <div class="pageTop">
        <span class="welcomeMessage floatl">
        Create New <g:message code="domain.ScheduledExecution.title"/>
        </span>

        <span class="floatr buttonholder">
        <g:link controller="scheduledExecution" action="upload" class="button textbtn">Upload Definition&hellip;</g:link>
        </span>

    </div>
    <g:render template="/scheduledExecution/createForm" model="[scheduledExecution:scheduledExecution,crontab:crontab,iscopy:iscopy,authorized:authorized]"/>
</body>
</html>
