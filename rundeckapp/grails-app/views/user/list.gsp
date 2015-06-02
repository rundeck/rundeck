<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
e<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - User List</title>
</head>

<body>
<div class="row " id="userListPage">
        <div class="col-sm-3">
            <g:render template="/menu/configNav" model="[selected: 'profiles']"/>
        </div>

        <div class="col-sm-9">
            <h3>Users

            <g:if test="${auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.ACTION_ADMIN], context: 'application')}">
                    <g:link action="create" class="btn btn-default btn-xs">
                        <i class="glyphicon glyphicon-plus"></i>
                        New Profile &hellip;
                    </g:link>
            </g:if></h3>
            <g:render template="/common/messages"/>


    <table cellpadding="0" cellspacing="0" width="100%" class="userlist">
        <g:each in="${users}" var="user" status="index">
            <tmpl:userListItem user="${user}" index="${index}"/>
        </g:each>
    </table>
    </div>

</div>
</body>
</html>


