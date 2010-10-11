<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="selectedMenu" content="Admin"/>
    <title><g:message code="main.app.name"/> - User Profile</title>
</head>

<body>

<div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage">User Profile: ${user.login}</span>
    </div>
    <span class="floatr">

        <g:form controller="user">
            <input type="hidden" name="login" value="${params.login}"/>
            <div id="schedShowButtons">
                <g:actionSubmit value="Edit"/>
            </div>

        </g:form>
    </span>
    <div class="clear"></div>
</div>

<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>

    <tmpl:user user="${user}"/>
</div>
</body>
</html>


