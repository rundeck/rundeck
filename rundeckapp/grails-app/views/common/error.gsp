<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>Error</title>

</head>
<body>

<div id="nowRunningContent">
    <div class="row">
    <div class="col-sm-12">
        <span class="h3 text-danger">${flash.title?:'Error'}</span>
    </div>
    </div>
</div>
<div class="row row-space">
<div class="col-sm-8 col-sm-offset-2">
    <g:render template="/common/messages" model='[notDismissable:true]'/>
</div>
</div>
</body>
</html>
