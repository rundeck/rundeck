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
<div  class="topbar solo" >
<g:if test="${session?.user && request.subject}">
<!--<div class="secondbar">-->

   <span id="top_tabs">

        <g:set var="resselected" value=""/>
        <g:ifPageProperty name='meta.tabpage' >
        <g:ifPageProperty name='meta.tabpage' equals='nodes'>
           <g:set var="resselected" value="selected"/>
        </g:ifPageProperty>
        </g:ifPageProperty>
        <g:link controller="framework" action="nodes" class=" toptab ${resselected}"  style="height:29px">
           <g:message code="gui.menu.Run"/>
        </g:link>

        <g:set var="wfselected" value=""/>
        <g:ifPageProperty name='meta.tabpage' >
        <g:ifPageProperty name='meta.tabpage' equals='jobs'>
           <g:set var="wfselected" value="selected"/>
        </g:ifPageProperty>
        </g:ifPageProperty>
        <g:link controller="menu" action="jobs" class=" toptab ${wfselected}" style="height:29px">
           <g:message code="gui.menu.Workflows"/>
        </g:link>

        <g:set var="eventsselected" value=""/>
        <g:ifPageProperty name='meta.tabpage' >
        <g:ifPageProperty name='meta.tabpage' equals='events'>
            <g:set var="eventsselected" value="selected"/>
        </g:ifPageProperty>
        </g:ifPageProperty>
        <g:link controller="reports"  action="index" class=" toptab ${eventsselected}"  style="height:29px">
            <g:message code="gui.menu.Events"/>
        </g:link>

    <g:if test="${session?.project||session?.projects}">
       <span class="projects" style="font-size:9pt; line-height: 12px; margin-left:20px;">
            <img src="${resource(dir:'images',file:'icon-tiny-rarrow-sep.png')}" alt="project: " width="7px" height="12px"/>
            <span id="projectSelect">
               <span class="action textbtn" onclick="loadProjectSelect();" title="Select project...">${session?.project?session.project:'Select project&hellip;'}</span>
            </span>
       </span>
    </g:if>

</span>
</g:if>
    <a href="${grailsApplication.config.rundeck.gui.titleLink?grailsApplication.config.rundeck.gui.titleLink:g.resource(dir:'/')}"
        title="Home" class="home" style="height:29px;">
        <g:set var="appTitle" value="${grailsApplication.config.rundeck.gui.title?grailsApplication.config.rundeck.gui.title:g.message(code:'main.app.name')}"/>
        <g:set var="appLogo" value="${grailsApplication.config.rundeck.gui.logo?grailsApplication.config.rundeck.gui.logo:g.message(code:'main.app.logo')}"/>
        <g:set var="appLogoW" value="${grailsApplication.config.rundeck.gui.'logo-width'?grailsApplication.config.rundeck.gui.'logo-width':g.message(code:'main.app.logo.width')}"/>
        <g:set var="appLogoH" value="${grailsApplication.config.rundeck.gui.'logo-height'?grailsApplication.config.rundeck.gui.'logo-height':g.message(code:'main.app.logo.height')}"/>
        <img src="${resource(dir:'images',file:appLogo)}" alt="${appTitle}" width="${appLogoW}" height="${appLogoH}"/>
        ${appTitle}
    </a>

<!--</div>-->
    %{--<g:if test="${session?.project}">
        <img src="${resource(dir:'images',file:'icon-tiny-rarrow-sep.png')}" alt="project: " width="7px" height="12px"/>
        <span class="projectinfo">
            <g:link controller="framework" action="showFrameworkProject" params="[project:session.project]" title="Select a project">${session.project}</g:link>
        </span>
    </g:if>--}%
    <g:if test="${session?.user && request.subject}">
        <span class="headright">
            <g:set var="adminauth" value="${false}"/>
            <g:if test="${session.project}">
            <g:set var="adminauth" value="${auth.resourceAllowedTest(type:'project',name:session.project,action:[AuthConstants.ACTION_ADMIN,AuthConstants.ACTION_READ],context:'application')}"/>
            <g:if test="${adminauth}">
                <g:link controller="menu" action="admin"><img src="${resource(dir:'images',file:'icon-small-admin.png')}" width="16px" height="16px" alt=""/>
                    Admin</g:link>
            </g:if>
            </g:if>
            <span class="logininfo">
                <g:if test="${adminauth}">
                    <img src="${resource(dir:'images',file:'icon-small-user-admin.png')}" width="16px" height="16px" alt=""/>
                </g:if>
                <g:else>
                    <img src="${resource(dir:'images',file:'icon-small-user.png')}" width="16px" height="16px" alt=""/>
                </g:else>
                <span class="userName" title="User ${session.user} is currently logged in.">
                    <g:link controller="user" action="profile">${session.user}</g:link>
                </span> &raquo;
                <g:link action="logout" controller="user" title="Logout user: ${session.user}" params="${[refLink:controllerName&&actionName?createLink(controller:controllerName,action:actionName,params:params,absolute:true):'']}">logout</g:link>
            </span>
            <a href="${org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.helpLink ? org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.helpLink : resource(dir:'docs')}" class="help">
                help
                <img src="${resource(dir:'images',file:'icon-small-help.png')}" width="16px" height="16px" alt=""/>
            </a>
        </span>
    </g:if>
    <g:else>
        <span class="headright">
            <a href="${org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.helpLink ? org.codehaus.groovy.grails.commons.ConfigurationHolder.config.rundeck?.gui?.helpLink : resource(dir: 'docs')}" class="help">
                help
                <img src="${resource(dir:'images',file:'icon-small-help.png')}" width="16px" height="16px" alt=""/>
            </a>
        </span>
    </g:else>
</div>


