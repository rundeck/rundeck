<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message
            code="ScheduledExecution.page.edit.title"/></title>
    <asset:javascript src="leavePageConfirm.js"/>
    <asset:javascript src="jobEditPage_bundle.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>
        var workflowEditor = new WorkflowEditor();
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        _onJobEdit(confirm.setNeedsConfirm);
    </g:javascript>
</head>
<body>


    <tmpl:editForm model="[scheduledExecution:scheduledExecution,crontab:crontab,authorized:authorized, notificationPlugins: notificationPlugins]"/>
</body>
</html>
