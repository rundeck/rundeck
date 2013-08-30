<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html class="" lang="en"><!--<![endif]-->
<head>
    <title><g:message code="main.app.name"/> - Login</title>
    <META HTTP-EQUIV="Pragma" CONTENT="no-cache">
    <META HTTP-EQUIV="Expires" CONTENT="-1">

    <asset:link rel="SHORTCUT" href="images/app-logo-small.png"/>
    <asset:link rel="favicon" href="images/app-logo-small.png"/>
    <asset:link rel="shortcut icon" href="images/app-logo-small.png"/>
    <asset:stylesheet href="rundeck.less"/>
    <asset:javascript src="application.js"/>
    <g:render template="/common/js"/>
    <g:render template="/common/css"/>
    <!--[if IE 7]>
    <asset:stylesheet href="ie7css.less"/>
    <![endif]-->
    <script language="javascript">
        //<!--
        Event.observe(window, 'load', loadFocus, false);

        function loadFocus() {
            $('login').focus();
        }
        if (typeof(oopsEmbeddedLogin) == 'function') {
            oopsEmbeddedLogin();
        }
        //-->
    </script>
</head>
<body id="loginpage">

<g:render template="/common/topbar"/>
<div class="container">

<div class="col-sm-4 col-sm-push-4">
    <div class="panel panel-primary ">
        <div class="panel-body">
        <form action="j_security_check" method="post" class="form " role="form">
            <g:set var="loginmsg" value="${grailsApplication.config.rundeck?.gui?.login?.welcome ?: g.message(code: 'gui.login.welcome', default: '')}"/>
            <g:if test="${loginmsg}">
            <div class="row">
                <span class="login welcome">
                   ${loginmsg}
                </span>
            </div>
            </g:if>
            <div class="form-group">
                <label for="login">Username</label>
                <input type="text" name="j_username" id="login" class="form-control" autofocus="true"/>
            </div>

            <div class="form-group">
                <label for="password">Password</label>
                <input type="password" name="j_password" id="password" class="form-control"/>
            </div>

            <div class="form-group">
                <button type="submit" class="btn btn-default">Login</button>
            </div>
        </form>
        </div>
        <g:if test="${flash.error}">
            <div class="panel-footer panel-danger">
                ${flash.error}
            </div>
        </g:if>
    </div>
</div>
</div>
<div class="container footer">
    <g:render template="/common/footer"/>
</div>
</body>
</html>
