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
        <b><g:enc>${exception.message}</g:enc></b>
    </div>
    <g:expander key="internalerror">Error Details</g:expander>
    <div style="display:none" id="internalerror">
        <div class="message">
            <div><b>Request:</b> <g:enc>${null != controllerName && null != actionName ? createLink(controller: controllerName, action: actionName, params: params) : request.getRequestURL()}</g:enc>
            </div>
            <strong>Message:</strong> <g:enc>${exception.message}</g:enc> <br/>
            <g:set var="hideStacktrace" value="${(grailsApplication.config?.rundeck?.gui?.errorpage?.hidestacktrace in [true,'true']) ||
                 Boolean.valueOf(System.getProperty("org.rundeck.gui.errorpage.hidestacktrace", "false"))}"/>
            <g:if test="${!hideStacktrace}">
                <strong>Caused by:</strong> <g:enc>${exception.cause?.message}</g:enc> <br/>
                <strong>Class:</strong> <g:enc>${exception.className}</g:enc> <br/>
                <strong>At Line:</strong> [<g:enc>${exception.lineNumber}</g:enc>] <br/>
                <strong>Code Snippet:</strong><br/>

                <div class="snippet">
                    <g:each var="cs" in="${exception.codeSnippet}">
                        <g:enc>${cs}</g:enc><br/>
                    </g:each>
                </div>
            </g:if>
        </div>
        <g:if test="${!hideStacktrace}">
            <h3>Stack Trace</h3>
            <pre><g:enc>${exception.stackTraceText}</g:enc></pre>
        </g:if>
    </div>
</div>
</body>
</html>
