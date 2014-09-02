<%
    response.setStatus(403,"Unauthorized") 
%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>Unauthorized Execution</title>

</head>
<body>

<div id="nowRunningContent">
    <div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage error">Unauthorized Execution</span>
    </div>
    <div class="clear"></div>
    </div>
</div>
<div class="pageBody" id="execUnauthorized">
    <g:if test="${scheduledExecution}">
        <div id="error" class="error note" >
            <g:if test="${scheduledExecution.id}">
                <g:link controller="scheduledExecution" action="show" id="${scheduledExecution.extid}"><g:enc>${scheduledExecution.jobName}</g:enc></g:link> cannot be executed:
            </g:if>
            <g:else>
                <g:enc>${scheduledExecution.jobName}</g:enc> cannot be executed:
            </g:else>
            User <g:enc>${session.user}</g:enc> is not authorized to execute the job

        </div>
    </g:if>
    <g:elseif test="${flash.error}">
        <div id="error" class="error note" >
            <g:enc>${flash.error}</g:enc>
        </div>
    </g:elseif>

</div>
</body>
</html>
