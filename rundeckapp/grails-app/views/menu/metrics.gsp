<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/3/13
  Time: 12:19 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="base"/>
    <meta name="meta.tabpage" content="configure"/>
    <title>Metrics Links</title>
</head>
<body>

<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'metrics']"/>
    </div>

    <div class="col-sm-9">
<g:markdown>
* [Metrics](${g.createLink(uri: '/metrics/metrics?pretty=true')}) (json)
* [Ping](${g.createLink(uri:'/metrics/ping')})
* [Threads](${g.createLink(uri: '/metrics/threads')})
* [Healthcheck](${g.createLink(uri: '/metrics/healthcheck')})  (json)
</g:markdown>
        </div>
    </div>

</body>
</html>
