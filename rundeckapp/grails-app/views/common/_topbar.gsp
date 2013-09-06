<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:set var="selectParams" value="${[:]}"/>
<g:if test="${pageScope._metaTabPage}">
    <g:set var="selectParams" value="${[page: _metaTabPage]}"/>
</g:if>
<nav class="navbar navbar-default navbar-static-top" role="navigation">

    <a href="${grailsApplication.config.rundeck.gui.titleLink ? grailsApplication.config.rundeck.gui.titleLink : g.resource(dir: '/')}"
       title="Home" class="navbar-brand">
        <g:set var="appTitle"
               value="${grailsApplication.config.rundeck.gui.title ? grailsApplication.config.rundeck.gui.title : g.message(code: 'main.app.name')}"/>
        <g:set var="appLogo"
               value="${grailsApplication.config.rundeck.gui.logo ? grailsApplication.config.rundeck.gui.logo : g.message(code: 'main.app.logo')}"/>
        <g:set var="appLogoW"
               value="${grailsApplication.config.rundeck.gui.'logo-width' ? grailsApplication.config.rundeck.gui.'logo-width' : g.message(code: 'main.app.logo.width')}"/>
        <g:set var="appLogoH"
               value="${grailsApplication.config.rundeck.gui.'logo-height' ? grailsApplication.config.rundeck.gui.'logo-height' : g.message(code: 'main.app.logo.height')}"/>
        <img src="${resource(dir: 'images', file: appLogo)}" alt="${appTitle}" width="${appLogoW}"
             height="${appLogoH}"/>
        ${appTitle}
    </a>

    <ul class="nav navbar-nav">
<g:if test="${session?.user && request.subject}">

        <g:set var="selectedclass" value="active"/>

        <g:set var="wfselected" value=""/>
        <g:ifPageProperty name='meta.tabpage' >
        <g:ifPageProperty name='meta.tabpage' equals='jobs'>
           <g:set var="wfselected" value="${selectedclass}"/>
        </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="resselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='nodes'>
                <g:set var="resselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="eventsselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='events'>
                <g:set var="eventsselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>

        <li class="${wfselected}"><g:link controller="menu" action="jobs" class=" toptab ${wfselected}" >
           <g:message code="gui.menu.Workflows"/>
        </g:link></li><!--
        --><li class="${resselected}"><g:link controller="framework" action="nodes" class=" toptab ${resselected}" >
           <g:message code="gui.menu.Nodes"/>
       </g:link></li><!--
        --><li class="${eventsselected}"><g:link controller="reports"  action="index" class=" toptab ${eventsselected}"  >
            <g:message code="gui.menu.Events"/>
        </g:link></li>

    <g:if test="${session?.project||session?.projects}">
        <g:if test="${session.frameworkProjects}">
            <li class="dropdown" id="projectSelect" >
            <g:render template="/framework/projectSelect" model="${[projects:session.frameworkProjects,project:session.project, selectParams: selectParams]}"/>
            </li>
        </g:if>
        <g:else>
            <li id="projectSelect" class="dropdown">
               <span class="action textbtn button" onclick="loadProjectSelect();" title="Select project...">${session?.project?session.project:'Select project&hellip;'}
               </span>
            </li>
        </g:else>
    </g:if>

    <g:unless test="${session.frameworkProjects}">
        <g:javascript>
            jQuery(window).load(function(){
                jQuery('#projectSelect').load('${createLink(controller: 'framework', action: 'projectSelect', params: selectParams)}');
            });
        </g:javascript>
    </g:unless>

</g:if>
</ul>
  <ul class="nav navbar-nav navbar-right">
    <g:set var="helpLinkUrl" value="${g.helpLinkUrl()}"/>
    <g:if test="${session?.user && request.subject}">
        <li class="headright">
            <g:set var="adminauth" value="${false}"/>
            <g:if test="${session.project}">
            <g:set var="adminauth" value="${auth.resourceAllowedTest(type:'project',name:session.project,action:[AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_READ],context:'application')}"/>
            <g:ifPageProperty name='meta.tabpage'>
                <g:ifPageProperty name='meta.tabpage' equals='configure'>
                    <g:set var="cfgselected" value="active"/>
                </g:ifPageProperty>
            </g:ifPageProperty>
            <g:if test="${adminauth}">
            <li class="${cfgselected ?: ''}">
                <g:link controller="menu" action="admin" ><g:message code="gui.menu.Admin"/></g:link>
            </li>
                <!-- --></g:if><!--
        --></g:if><!--
            -->
        <li class="dropdown">
            <g:link controller="user" action="profile" class="dropdown-toggle" data-toggle="dropdown" data-target="#" id="userLabel"
                    role="button">
                ${session.user.encodeAsHTML()} <span class="caret"></span>
            </g:link>
            <ul class="dropdown-menu" role="menu" aria-labelledby="userLabel">
                <li><g:link controller="user" action="profile">Profile</g:link></li>
                <li class="divider"></li>
                <li><g:link action="logout" controller="user" title="Logout user: ${session.user}"
                            params="${[refLink: controllerName && actionName ? createLink(controller: controllerName, action: actionName, params: params, absolute: true) : '']}">
                    Logout
                    <b class="glyphicon glyphicon-remove"></b>
                </g:link>
                </li>
            </ul>
        </li>
        <li>
            <a href="${helpLinkUrl.encodeAsHTML()}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:if>
    <g:else>
        <li >
            <a href="${helpLinkUrl.encodeAsHTML()}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:else>
    </ul>
</nav>
