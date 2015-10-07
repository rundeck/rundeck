<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="userController.page.edit.title"/></title>

</head>

<body>
<div class="row">
    <div class="col-sm-offset-1 col-sm-10">
        <g:form action="update" class="form form-horizontal" useToken="true">
            <div class="panel panel-primary">
                <div class="panel-heading">
                    <h3 class="panel-title"><g:message code="userController.page.edit.title"/></h3>
                </div>

                <div class="panel-body">

                    <g:render template="/common/messages"/>
                    <tmpl:edit user="${user}"/>
                </div>

                <div class="panel-footer">
                    <g:actionSubmit id="editFormCancelButton" value="Cancel" class="btn btn-default"/>
                    <g:submitButton name="Update" class="btn btn-primary"/>
                </div>
            </div>
        </g:form>
    </div>
</div>
</body>
</html>


