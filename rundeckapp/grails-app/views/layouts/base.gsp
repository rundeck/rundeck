<!DOCTYPE html>
<!--[if lt IE 7 ]> <html class="ie6"> <![endif]-->
<!--[if IE 7 ]>    <html class="ie7"> <![endif]-->
<!--[if IE 8 ]>    <html class="ie8"> <![endif]-->
<!--[if IE 9 ]>    <html class="ie9"> <![endif]-->
<!--[if (gt IE 9)|!(IE)]><!--> <html class="" lang="en"><!--<![endif]-->
<head>
    <title>
        <g:layoutTitle default="${g.message(code:'main.app.name')}"/>
    </title>
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


    <script type="text/javascript" src="${resource(dir:'js',file:'application.js')}"></script>
    <script type="text/javascript" src="${resource(dir:'js',file:'menus.js')}"></script>
    <g:if test="${pageProperty(name:'meta.rssfeed')}">
        <g:ifServletContextAttribute attribute="RSS_ENABLED" value="true">
            <link rel="alternate" type="application/rss+xml" title="RSS 2.0" href="${pageProperty(name:'meta.rssfeed')}"/>
        </g:ifServletContextAttribute>
    </g:if>
    <g:layoutHead/>
</head>
<body>
<div id="wrap">

	<div id="main">

<g:render template="/common/topbar"/>
<g:render template="/common/pageMessage"/>

    <g:layoutBody/>
	</div>

</div>

<div id="footer">
<g:render template="/common/footer"/>

</div>

</body>
</html>
