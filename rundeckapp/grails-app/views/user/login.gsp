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

    <link rel="SHORTCUT ICON" href="${resource(dir:'images',file:'app-logo-small.png')}"/>
    <link rel="favicon" href="${resource(dir:'images',file:'app-logo-small.png')}"/>
    <link rel="icon" href="${resource(dir:'images',file:'app-logo-small.png')}" type="image/x-icon" />
    <link rel="shortcut icon" href="${resource(dir:'images',file:'app-logo-small.png')}" type="image/x-icon" />
    <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}"/>
    <link rel="stylesheet" href="${resource(dir:'css',file:'menus.css')}"/>

    <g:javascript library="prototype-1.7.0.0"/>
    <g:render template="/common/js"/>
    <g:render template="/common/css"/>
    <!--[if IE 7]>
    <link rel="stylesheet" type="text/css" href="${resource(dir:'css',file:'ie7css.css')}" />
    <![endif]-->
    <script type="text/javascript" src="${resource(dir:'js',file:'application.js')}"></script>
    <script type="text/javascript" src="${resource(dir:'js',file:'menus.js')}"></script>
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

<div id="wrap">

	<div id="main">
<g:render template="/common/topbar"/>

<div class="login">
    <form action="j_security_check" method="post">
        <g:set var="loginmsg" value="${grailsApplication.config.rundeck?.gui?.login?.welcome ?: g.message(code: 'gui.login.welcome', default: '')}"/>
        <g:if test="${loginmsg}">
        <div class="row">
            <span class="login welcome">
               ${loginmsg}
            </span>
        </div>
        </g:if>
        <div class="row">
            <label for="login">Username: </label>
            <span class="input"><input type="text" name="j_username" id="login"/></span>
        </div>
        <div class="row">
            <label for="password">Password: </label>
            <span class="input"><input type="password" name="j_password" id="password"/></span>
        </div>
        <div class="row">
            <span class="input"><input type="submit" value="Login"/></span>
        </div>
        <g:if test="${flash.error}">
            <div class="message">
                <span class="error">${flash.error}</span>
            </div>
        </g:if>
        <div class="clear"></div>
    </form>
</div>
</div>
</div>
<div id="footer">
    <g:render template="/common/footer"/>
</div>
</body>
</html>
