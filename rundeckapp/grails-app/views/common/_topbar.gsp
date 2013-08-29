<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<script type="text/javascript">
//<!--
var menus = new MenuController();
function loadProjectSelect(){
    new Ajax.Updater('projectSelect','${createLink(controller:'framework',action:'projectSelect')}',{
        evalScripts:true
    });
}
function selectProject(value){
    if(value=='-new-'){
        doCreateProject();
        return;
    }

    new Ajax.Request('${createLink(controller:'framework',action:'selectProject')}',{
        evalScripts:true,
        parameters:{project:value},
        onSuccess:function(transport){
            $('projectSelect').loading(value?value:'All projects...');
            if(typeof(_menuDidSelectProject)=='function'){
                if(_menuDidSelectProject(value)){
                    loadProjectSelect();
                }
            }else{
                oopsEmbeddedLogin();
            }
        }
    });
}
function doCreateProject(){
    document.location = "${createLink(controller:'framework',action:'createProject')}";
}
//-->
</script>
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
       <li><span class="navbar-text" >
            <span id="projectSelect">
                <g:if test="${session.frameworkProjects}">
                    <g:render template="/framework/projectSelect" model="${[projects:session.frameworkProjects,project:session.project]}"/>
                </g:if>
                <g:else>
                   <span class="action textbtn button" onclick="loadProjectSelect();" title="Select project...">${session?.project?session.project:'Select project&hellip;'}
                    <img src="${resource(dir: 'images', file: 'icon-tiny-disclosure.png')}" alt="project: " width="12px"
                         height="12px"/>
                   </span>
                </g:else>
            </span>
       </span></li>
    </g:if>

    <g:unless test="${session.frameworkProjects}">
        <g:javascript>
            fireWhenReady('projectSelect', loadProjectSelect);
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
                    <g:set var="cfgselected" value="selected"/>
                </g:ifPageProperty>
            </g:ifPageProperty>
            <g:if test="${adminauth}">
            <li>
                <g:link controller="menu" action="admin" class=" toptab ${cfgselected?:''}"><g:message code="gui.menu.Admin"/></g:link>
            </li>
                <!-- --></g:if><!--
        --></g:if><!--
            -->
        <li class="dropdown">
            <a class="dropdown-toggle" data-toggle="dropdown">
                ${session.user.encodeAsHTML()} <span class="glyphicon glyphicon-chevron-down"></span>
            </a>
            <ul class="dropdown-menu">
                <li><g:link controller="user" action="profile">Profile</g:link></li>
                <li><g:link action="logout" controller="user" title="Logout user: ${session.user}"
                            params="${[refLink: controllerName && actionName ? createLink(controller: controllerName, action: actionName, params: params, absolute: true) : '']}">logout</g:link>
                </li>
            </ul>
        </li>
        <li>
            <!--
        --><a href="${helpLinkUrl.encodeAsHTML()}" class="help sepL">
                help
            </a>
        </li>
    </g:if>
    <g:else>
        <li class="headright">
            <a href="${helpLinkUrl.encodeAsHTML()}" class="help  sepL">
                help
            </a>
        </li>
    </g:else>
    </ul>
</nav>
