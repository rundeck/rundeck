<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>${flash.title ?: title ?: 'Error'}</title>

</head>
<body>

<div class="pageTop">
    <span class="welcomeMessage error">${flash.title?:title?:'Error'}</span>
</div>
<div class="pageBody" id="execUnauthorized">
    <g:render template="/common/errorFragment" model="${[error:error,message:message]}"/>
</div>
</body>
</html>
