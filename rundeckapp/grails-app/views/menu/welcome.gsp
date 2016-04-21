<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 4/18/16
  Time: 12:54 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>

    <meta name="layout" content="base"/>
    <meta name="tabpage" content="home"/>
    <title><g:appTitle/><g:message code="page.welcome.title.suffix" /></title>
</head>

<body>

<div class="row">
    <div class="col-sm-12">
        <div class="jumbotron">
            <h2>
                <g:message code="app.firstRun.title"
                           args="${[g.appTitle(), grailsApplication.metadata['build.ident']]}"/>
            </h2>

            <g:markdown><g:autoLink>${message(code: "app.firstRun.md")}</g:autoLink></g:markdown>

            <g:link controller="menu" action="index" class="btn btn-lg btn-success">
                <g:message code="welcome.button.use.rundeck" />
            </g:link>
        </div>
    </div>
</div>
</body>
</html>