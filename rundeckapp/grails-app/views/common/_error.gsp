<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>Error</title>

</head>
<body>

<div class="pageTop">
    <span class="welcomeMessage error">${flash.title?flash.title:'Error'}</span>
</div>
<div class="pageBody" id="execUnauthorized">
    <g:if test="${flash.error || flash.message || error || message}">
        <div id="error" class="error note" >
            ${flash.error}${flash.message}${error}${message}
        </div>
    </g:if>
</div>
</body>
</html>
