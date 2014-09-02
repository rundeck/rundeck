<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <g:set var="pageTitle" value="${flash.title ?: request.title ?: request.titleCode ? g.message(code: request.titleCode) :
        g.message(code: 'request.error.title')}"/>
    <title><g:enc>${pageTitle}</g:enc></title>

</head>
<body>

<div class="row row-space">
<div class="col-sm-6 col-sm-offset-3">
    <div class="panel panel-default">
    <div class="panel-heading">
    <h2 class="panel-title "><g:enc>${pageTitle}</g:enc></h2>
    </div>
    <div class="panel-body text-danger">
        <g:render template="/common/messagesText"/>
    </div>
    </div>
</div>
</div>

</body>
</html>
