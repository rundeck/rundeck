<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html lang="en"><!--<![endif]-->
<head>
    <title>
        <g:layoutTitle default="${g.message(code:'main.app.name')}"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="SHORTCUT" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="favicon" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <link rel="shortcut icon" href="${g.resource(dir: 'images', file: 'favicon.ico')}"/>
    <link rel="apple-touch-icon-precomposed" href="${g.resource(dir: 'images', file: 'favicon-152.png')}"/>
    <asset:stylesheet href="rundeck.css"/>
    <asset:stylesheet href="non_responsive.css"/>
    <!--[if lt IE 9]>
    <g:javascript library="respond.min"/>
    <![endif]-->
    <asset:javascript src="jquery.js"/>
    <asset:javascript src="bootstrap.js"/>
    <g:javascript library="prototype"/>
    <g:javascript library="application"/>
    <g:javascript library="menus"/>
    <g:render template="/common/js"/>
    <g:render template="/common/css"/>

    <script language="javascript">
        function oopsEmbeddedLogin() {
        <%
            if (g.pageProperty(name: 'meta.tabpage')) { %>
                document.location = '${createLink(controller:"menu",params:params+[page:g.pageProperty(name: 'meta.tabpage')])}';
            <%
            } else { %>
                document.location = '${createLink(controller:"menu")}';
            <%
            }
        %>
        }
    </script>
    <g:ifPageProperty name="meta.tabpage">
        <g:set var="_metaTabPage" value="${g.pageProperty(name: 'meta.tabpage')}" scope="page"/>
    </g:ifPageProperty>

    <g:if test="${pageProperty(name:'meta.rssfeed')}">
        <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
            <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${pageProperty(name:'meta.rssfeed')}"/>
        </g:ifServletContextAttribute>
    </g:if>
    <g:layoutHead/>
</head>
<body>
<g:render template="/common/topbar"/>
<div class="container">
    <g:render template="/common/pageMessage"/>
    <g:layoutBody/>
</div>

<div class="container footer">
<g:render template="/common/footer"/>
</div>

</body>
</html>
