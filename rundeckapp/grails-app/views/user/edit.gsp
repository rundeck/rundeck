<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - User Profile</title>

</head>
<body>

<div class="pageTop">
        <div class="floatl">
            <span class="welcomeMessage">Edit User Profile</span>
        </div>
</div
<div class="pageBody" id="userProfilePage">
    <g:render template="/common/messages"/>

        <g:form action="update">
        <tmpl:edit user="${user}"/>
        <div class="buttons">

            <g:actionSubmit id="editFormCancelButton" value="Cancel"/>
            <g:actionSubmit value="Update"/>

        </div>
</g:form>
</div>
</body>
</html>


