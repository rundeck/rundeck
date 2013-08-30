<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:message code="main.app.name"/> - User Profile</title>

</head>
<body>

<div class="row">
    <div class="col-sm-12">
        <h3>Create User Profile</h3>
    </div>
</div
<div class="row" id="userProfilePage">
    <g:render template="/common/messages"/>
    <div class="col-sm-6 col-sm-push-1">
        <g:form action="store" method="POST" class="form">
        <tmpl:edit user="${user}"/>
        <div class="form-group">

            <g:actionSubmit id="editFormCancelButton" value="Cancel" class="btn btn-default"/>
            <g:submitButton name="Create" class="btn btn-primary"/>

        </div>
    </div>
</g:form>
</div>
</body>
</html>


