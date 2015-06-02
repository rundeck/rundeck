<html>
<head>
    <title><g:appTitle/> - Error</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
</head>

<body>


<div class="container">
<div class="row">
<div class="col-sm-12">
    <div class="panel panel-default">
        <div class="panel-heading">
            <h2 class="panel-title ">An Error Occurred</h2>
        </div>

        <div class="panel-body text-danger">
            <b><g:enc>${exception.message}</g:enc></b>
        </div>
    </div>
<g:set var="hideStacktrace"
value="${(grailsApplication.config?.rundeck?.gui?.errorpage?.hidestacktrace in [true, 'true']) ||

               Boolean.valueOf(System.getProperty("org.rundeck.gui.errorpage.hidestacktrace", "false"))}"/>
<g:if test="${!hideStacktrace}">
    <div class="panel-group" id="accordion">
        <div class="panel panel-default">
            <div class="panel-heading">
                <h4 class="panel-title">
                    <a data-toggle="collapse" data-parent="#accordion" href="#internalerror">
                        Error Details <i class="glyphicon glyphicon-chevron-right"></i>
                    </a>
                </h4>
            </div>

    <div class="panel-collapse collapse collapse-expandable" id="internalerror">
        <div class="panel panel-default">
        <div class="panel-body">
        <div class="container">
        <div class="row">
        <div class="col-sm-12">
            <div><b>Request:</b> <g:enc>${null != controllerName && null != actionName ? createLink(controller: controllerName, action: actionName, params: params) : request.getRequestURL()}</g:enc>
            </div>
            <strong>Message:</strong> <g:enc>${exception.message}</g:enc> <br/>
                <strong>Caused by:</strong> <g:enc>${exception.cause?.message}</g:enc> <br/>
                <strong>Class:</strong> <g:enc>${exception.className}</g:enc> <br/>
                <strong>At Line:</strong> [<g:enc>${exception.lineNumber}</g:enc>] <br/>
                <strong>Code Snippet:</strong><br/>

                <div class="snippet">
                    <g:each var="cs" in="${exception.codeSnippet}">
                        <g:enc>${cs}</g:enc><br/>
                    </g:each>
                </div>
        </div>
        <h3>Stack Trace</h3>
        <pre><g:enc>${exception.stackTraceText}</g:enc></pre>
    </div>
    </div>
    </div>
    </div>
    </div>
    </div>
    </div>
</g:if>
</div>
</div>
</div>
</body>
</html>
