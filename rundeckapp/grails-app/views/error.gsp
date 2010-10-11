<html>
<head>
    <title><g:message code="main.app.name"/> - Error</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <style type="text/css">
    #internalerror {
        border: 1px solid black;
        padding: 10px;
    }

    .errorMessage {
        color: red;
        padding: 5px;
        font-style: normal;
        color: #F55;
        border: 1px solid #fdd;
        background: #f0f0f0;
        -moz-border-radius: 5px;
        -webkit-border-radius: 5px;
        border-radius: 5px;
        margin-bottom: 15px;
    }
    .welcomeMessage.error{
        font-size: 12pt;
        color: black;
    }
    </style>
</head>

<body>
<div class="pageTop">
    <div class="welcomeMessage error">An Error Occurred</div>
</div>
<div class="pageBody">
    <div class="errorMessage">
        <b>${exception.message}</b>
    </div>
    <g:expander key="internalerror">Error Details</g:expander>
    <div style="display:none" id="internalerror">
        <div class="message">
            <div><b>Request:</b> ${null != controllerName && null != actionName ? createLink(controller: controllerName, action: actionName, params: params) : request.getRequestURL()}</div>
            <strong>Message:</strong> ${exception.message} <br/>
            <strong>Caused by:</strong> ${exception.cause?.message} <br/>
            <strong>Class:</strong> ${exception.className} <br/>
            <strong>At Line:</strong> [${exception.lineNumber}] <br/>
            <strong>Code Snippet:</strong><br/>
            <div class="snippet">
                <g:each var="cs" in="${exception.codeSnippet}">
                    ${cs}<br/>
                </g:each>
            </div>
        </div>
        <h3>Stack Trace</h3>
        <pre>${exception.stackTraceText}</pre>
    </div>
</div>
</body>
</html>