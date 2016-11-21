%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%

<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:set var="selectParams" value="${[:]}"/>
<g:if test="${pageScope._metaTabPage}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
</g:if>
<nav class="navbar-overrides navbar navbar-default navbar-static-top" role="navigation">

    <div class="navbar-header">
        <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
            <span class="sr-only">Toggle navigation</span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
        </button>
    <a href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}"
       title="Home" class="navbar-brand">
        <g:set var="appTitle"
               value="${grailsApplication.config.rundeck?.gui?.title ?: g.message(code: 'main.app.name',default:'')}"/>
        <g:set var="appDefaultTitle" value="${g.message(code: 'main.app.default.name',default:'')}"/>
        <g:set var="brandHtml"
               value="${grailsApplication.config.rundeck?.gui?.brand?.html ?: g.message(code: 'main.app.brand.html',default:'')}"/>
        <g:set var="brandDefaultHtml"
               value="${g.message(code: 'main.app.brand.default.html',default:'')}"/>
        <i class="rdicon app-logo"></i>
        <g:if test="${brandHtml}">
            ${enc(sanitize:brandHtml)}
        </g:if>
        <g:elseif test="${appTitle}">
            ${appTitle}
        </g:elseif>
        <g:elseif test="${brandDefaultHtml}">
            ${enc(sanitize:brandDefaultHtml)}
        </g:elseif>
        <g:else>
            ${appDefaultTitle}
        </g:else>
    </a>
    </div>

    <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='configure'>
                <g:set var="cfgselected" value="active"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
    </g:if>
    <div class="container">
    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
    <ul class="nav navbar-nav">
<g:if test="${session?.user && request.subject }">
    <g:set var="homeselected" value="${false}"/>
    <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
    <g:ifPageProperty name='meta.tabpage'>
        <g:ifPageProperty name='meta.tabpage' equals='home'>
            <g:set var="homeselected" value="${true}"/>
        </g:ifPageProperty>
    </g:ifPageProperty>
    </g:if>
    <g:if test="${! homeselected}">

    <g:if test="${params.project ?: request.project || session?.projects}">
        <g:if test="${session.frameworkProjects}">
            <li class="dropdown" id="projectSelect">
                <g:render template="/framework/projectSelect"
                          model="${[projects: session.frameworkProjects, project: params.project ?: request.project, selectParams: selectParams]}"/>
            </li>
        </g:if>
        <g:else>
            <li id="projectSelect" class="dropdown disabled">
                <a data-toggle="dropdown" href="#" class="disabled">
                    <i class="caret"></i>
                </a>
            </li>
        </g:else>
        <li id="projectHomeLink">
            <a href="${createLink(
                    controller: 'menu',
                    action: 'projectHome',
                    params: [project: project ?: params.project ?: request.project]
            )}">
                <i class="glyphicon glyphicon-tasks"></i>
                <g:enc>${project ?: params.project ?: request.project ?: 'Choose ...'}</g:enc>
            </a>
        </li>
    </g:if>

        <g:set var="selectedclass" value="active"/>

        <g:set var="wfselected" value=""/>
        <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
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
        <g:set var="adhocselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='adhoc'>
                <g:set var="adhocselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        <g:set var="eventsselected" value=""/>
        <g:ifPageProperty name='meta.tabpage'>
            <g:ifPageProperty name='meta.tabpage' equals='events'>
                <g:set var="eventsselected" value="${selectedclass}"/>
            </g:ifPageProperty>
        </g:ifPageProperty>
        </g:if>
        <g:if test="${params.project?:request.project}">
        <li class="${enc(attr:wfselected)}"><g:link controller="menu" action="jobs" class=" toptab ${enc(attr: wfselected)}"
                                          params="[project: params.project ?: request.project]">
           <g:message code="gui.menu.Workflows"/>
        </g:link></li><!--
        --><li class="${enc(attr:resselected)}"><g:link controller="framework" action="nodes" class=" toptab ${enc(attr: resselected)}"
                                              params="[project: params.project ?: request.project]" >
           <g:message code="gui.menu.Nodes"/>
       </g:link></li><!--
        --><g:if
                test="${auth.adhocAllowedTest(action:AuthConstants.ACTION_RUN,project: params.project?:request.project)}"><li
                    class="${enc(attr:adhocselected)}"><g:link
                controller="framework" action="adhoc"
                                                  class=" toptab ${enc(attr:adhocselected)}"
                                                params="[project: params.project ?: request.project]">
           <g:message code="gui.menu.Adhoc"/>
       </g:link></li></g:if><!--
        --><li class="${enc(attr:eventsselected)}"><g:link controller="reports"  action="index" class=" toptab ${enc(attr:eventsselected)}"
                                                 params="[project: params.project ?: request.project]" >
            <g:message code="gui.menu.Events"/>
        </g:link></li>
        </g:if>

    <g:unless test="${session.frameworkProjects}">
        <g:javascript>
            jQuery(function(){
                jQuery('#projectSelect').load('${enc(js:createLink(controller: 'framework', action: 'projectSelect', params: selectParams))}',{},function(x,r){
                    jQuery('#projectSelect').removeClass('disabled');
                });
            });
        </g:javascript>
    </g:unless>
    </g:if>
</g:if>
</ul>
<g:if test="${session?.user && request.subject }">
<g:ifExecutionMode passive="true">
    <p class="navbar-text has_tooltip navbar-text-warning"
       title="${g.message(code:'system.executionMode.description.passive')}"
       data-toggle="tooltip"
       data-placement="bottom"
    >
        <i class="glyphicon glyphicon-exclamation-sign"></i>
        <g:message code="passive.mode" />
    </p>
    <auth:resourceAllowed action="${[AuthConstants.ACTION_ENABLE_EXECUTIONS,AuthConstants.ACTION_ADMIN]}" any="true" context="application" kind="system">
    <g:form class="navbar-form navbar-left" controller="execution" action="executionMode" method="POST" useToken="true">
        <g:hiddenField name="mode" value="active"/>
        <g:hiddenField name="project" value="${params.project}"/>
        <g:link action="executionMode"
                controller="menu"
                class="btn btn-default "
                title="${message(code:"action.executionMode.set.active.help")}"
        >
            Change
        </g:link>
    </g:form>
    </auth:resourceAllowed>
</g:ifExecutionMode>
</g:if>
  <ul class="nav navbar-nav navbar-right">
      <g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
          <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
              <g:if test="${grailsApplication.config.rundeck?.gui?.clusterIdentityInHeader in [true,'true']}">
                  <li>
                      <span class="navbar-text rundeck-server-uuid"
                            data-server-uuid="${ servletContextAttribute(attribute: 'SERVER_UUID')}"
                            data-server-name="${ servletContextAttribute(attribute: 'FRAMEWORK_NODE')}"
                      >
                      </span>
                  </li>
              </g:if>
          </g:ifServletContextAttribute>
      </g:ifServletContextAttributeExists>
    <g:set var="helpLinkUrl" value="${g.helpLinkUrl()}"/>
    <g:if test="${session?.user && request.subject}">
        <li class="headright">
            <g:set var="projConfigAuth" value="${false}"/>
            <g:if test="${params.project ?: request.project}">
            <g:set var="projConfigAuth" value="${auth.resourceAllowedTest(type:'project',name: (params.project ?: request.project),
                    action: [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_IMPORT,
                            AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_DELETE],any:true,context:'application')}"/>

            <g:if test="${projConfigAuth}">
            <li class="${cfgselected ?: ''}">
                <g:link controller="menu" action="admin" title="${g.message(code:'gui.menu.Admin')}"
                        params="[project: params.project?:request.project]">
                    <i class="glyphicon glyphicon-cog"></i>
                </g:link>
            </li>
                <!-- --></g:if><!--
        --></g:if><g:elseif
            test="${auth.resourceAllowedTest(type: 'resource', kind: 'system',
                    action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN], any: true, context: 'application')}">

        <li class="${cfgselected ?: ''}">
        %{--Generic config page no project selected--}%
            <g:link controller="menu" action="systemConfig" title="${g.message(code: 'gui.menu.Admin')}">
                <i class="glyphicon glyphicon-cog"></i>
            </g:link>
        </li>
    </g:elseif><!--
            -->
        <li class="dropdown">
            <g:link controller="user" action="profile" class="dropdown-toggle" data-toggle="dropdown" data-target="#" id="userLabel"
                    role="button">
                <g:enc>${session.user}</g:enc> <span class="caret"></span>
            </g:link>
            <ul class="dropdown-menu" role="menu" aria-labelledby="userLabel">
                <li><g:link controller="user" action="profile">
                        <i class="glyphicon glyphicon-user"></i>
                        Profile
                    </g:link>
                </li>
                <li class="divider"></li>
                <li><g:link action="logout" controller="user" title="Logout user: ${enc(attr:session.user)}">
                    <i class="glyphicon glyphicon-remove"></i>
                    Logout
                </g:link>
                </li>
            </ul>
        </li>
        <li>
            <a href="${enc(attr:helpLinkUrl)}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:if>
    <g:else>
        <li >
            <a href="${enc(attr:helpLinkUrl)}" class="help ">
                help <b class="glyphicon glyphicon-question-sign"></b>
            </a>
        </li>
    </g:else>
    </ul>
    </div>
    </div>
</nav>
