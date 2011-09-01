<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
e<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - User List</title>
</head>

<body>

<div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage">Users</span>

        <g:if test="${auth.resourceAllowedTest(kind:'user',action:[AuthConstants.ACTION_ADMIN],context:'application')}">
        <span class="buttons">
            <g:link action="create" class="button textaction">New Profile &hellip;</g:link>
        </span>
        </g:if>
    </div>
    <div class="clear"></div>
</div>

<div class="pageBody" id="userListPage">
    <g:render template="/common/messages"/>


    <table cellpadding="0" cellspacing="0" width="100%" class="userlist">
        <g:each in="${users}" var="user" status="index">
            <tmpl:userListItem user="${user}" index="${index}"/>
        </g:each>
    </table>

</div>
</body>
</html>


