<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; org.rundeck.core.auth.AuthConstants" %>
  <g:set var="selectParams" value="${[:]}"/>
  <g:if test="${pageScope._metaTabPage && pageScope._metaTabPage != 'configure'&& pageScope._metaTabPage != 'projectconfigure'}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
  </g:if>
  <nav id="mainbar" class="mainbar">
    <div id="nav-rd-home">
      <g:set var="titleLink" value="${g.rConfig(value: "gui.titleLink", type: 'string')}"/>
      <a href="${titleLink ? enc(attr:titleLink) : g.createLink(uri: '/')}">
        <i class="rdicon app-logo" style="display:block"></i>
      </a>
    </div>
    <!-- <div class="navbar-minimize">
      <button class="btn btn-fill btn-icon">
        <i class="fas fa-ellipsis-v"></i>
        <i class="fas fa-ellipsis-h"></i>
      </button>
    </div> -->
    <div class="mainbar__group" style="margin-left: 10px">
%{--    Weeeooo Weeeooo; this section under impound on authority of the [Re]Design police--}%
%{--      <button type="button" class="navbar-toggle">--}%
%{--        <span class="sr-only">Toggle navigation</span>--}%
%{--        <span class="icon-bar bar1"></span>--}%
%{--        <span class="icon-bar bar2"></span>--}%
%{--        <span class="icon-bar bar3"></span>--}%
%{--      </button>--}%

      <g:set var="userDefinedLogo" value="${g.rConfig(value: "gui.logo", type: 'string')}"/>
      <g:set var="userDefinedSmallLogo" value="${g.rConfig(value: "gui.logoSmall", type: 'string')}"/>
      <g:set var="userAssetBase" value="/user-assets" />
      <g:set var="safeUserLogo" value="${userDefinedLogo.toString().encodeAsSanitizedHTML()}" />
      <g:set var="safeUserSmallLogo" value="${userDefinedSmallLogo.toString().encodeAsSanitizedHTML()}" />

      <g:if test="${userDefinedLogo && !userDefinedSmallLogo}">
        <img src="${createLink(uri:userAssetBase+"/"+safeUserLogo)}" height="40px" style="float: left; margin-top:10px; margin-right: 20px;">

      </g:if>
      <g:elseif test="${userDefinedLogo && userDefinedSmallLogo}">
        <img src="${createLink(uri:userAssetBase+"/"+safeUserSmallLogo)}" height="40px" style="float: left; margin-top:10px; margin-right: 20px;">
      </g:elseif>

      <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
        <g:ifPageProperty name='meta.tabpage'>
          <g:ifPageProperty name='meta.tabpage' equals='configure'>
            <g:set var="cfgselected" value="active"/>
          </g:ifPageProperty>
        </g:ifPageProperty>
      </g:if>
      <g:if test="${session?.user && request.subject }">
        <ul class="nav navbar-nav">
          <g:set var="homeselected" value="${false}"/>
          <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
            <g:ifPageProperty name='meta.tabpage'>
              <g:ifPageProperty name='meta.tabpage' equals='home'>
                <g:set var="homeselected" value="${true}"/>
              </g:ifPageProperty>
            </g:ifPageProperty>
          </g:if>
          <g:if test="${!homeselected}">
            <g:set var="projconfigselected" value=""/>
              <g:if test="${project ?: params.project ?: request.project}">
                  <g:set var="projectName" value="${project ?: params.project ?: request.project}"/>
                  <g:set var="projectLabel" value="${session.frameworkLabels?.getAt(projectName)?: projectName}"/>

                  <li id="projectHomeLink" class="primarylink">
                      <g:link controller="menu" action="projectHome" params="[project: projectName]" style="display: none">
                          Project <i class="fas fa-chevron-right fa-xs primarylink-chevron"></i> <g:enc> ${projectLabel}</g:enc>
                      </g:link>
                      <div id="projectPicker" data-project-label="${projectLabel}"/>
                  </li>
            </g:if>
            <g:else>
              <div id="projectPicker" data-project-label=""/>
            </g:else>
            <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
              <g:ifPageProperty name='meta.tabpage'>
                <g:ifPageProperty name='meta.tabpage' equals='projectconfigure'>
                  <g:set var="projconfigselected" value="active"/>
                </g:ifPageProperty>
              </g:ifPageProperty>
            </g:if>
            <g:unless test="${session.frameworkProjects}">
              <g:javascript>
                jQuery(function(){ jQuery('#projectSelect').load('${enc(js:createLink(controller: 'framework', action: 'projectSelect', params: selectParams))}',{},function(x,r){ jQuery('#projectSelect').removeClass('disabled'); }); });
              </g:javascript>
            </g:unless>
          </g:if>
          <g:else>
            <div id="projectPicker" data-project-label=""/>
          </g:else>
        </ul>
      </g:if>
    </div>

    <div class="mainbar__group" style="margin-left: auto;">
        <g:if test="${(project?: params.project ?: request.project)}">
        <g:ifExecutionMode is="passive" project="${project?:params.project?:request.project}">
            <div class="text-warning  has_tooltip" data-placement="right" title="${message(code:'project.execution.disabled')}">
                <i class="glyphicon glyphicon-pause"></i>
            </div>
        </g:ifExecutionMode>
        <g:ifScheduleMode is="passive" project="${project?:params.project?:request.project}">
            <p class="text-warning has_tooltip"  data-placement="right" title="${message(code:'project.schedule.disabled')}">
                <i class="glyphicon glyphicon-ban-circle"></i>
            </p>
        </g:ifScheduleMode>

          <p class="vue-project-motd motd-indicator" style="margin: 0;">
            <motd-indicator :event-bus="EventBus"></motd-indicator>
          </p>

        </g:if>

      <ul id="navbar-menu" class="mainbar__group mainbar__menu">
        <g:set var="userDefinedInstanceName" value="${g.rConfig(value: "gui.instanceName", type: 'string')}"/>
        <g:if test="${userDefinedInstanceName}">
          <li>
            <span class="label label-default instance-label" style="float:left;font-size: 20px;margin: 10px 15px 0 0;">
                ${enc(sanitize:userDefinedInstanceName)}
            </span>
          </li>

        </g:if>


        <g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
          <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
            <g:set var="clusterIdentityInHeader" value="${g.rConfig(value: "gui.clusterIdentityInHeader", type: 'string')}"/>
            <g:if test="${clusterIdentityInHeader in [true,'true']}">
              <li>
                <span class="rundeck-server-uuid" data-server-uuid="${ servletContextAttribute(attribute: 'SERVER_UUID')}" data-server-name="${ servletContextAttribute(attribute: 'FRAMEWORK_NODE')}"></span>
              </li>
            </g:if>
          </g:ifServletContextAttribute>
        </g:ifServletContextAttributeExists>
        <g:if test="${session?.user && request.subject}">
          <g:ifExecutionMode passive="true">
            <li id="appExecMode">
              <span class="has_tooltip"
                 title="${g.message(code:'passive.mode')} - ${g.message(code:'system.executionMode.description.passive')}"
                 data-toggle="tooltip"
                 data-placement="bottom"
              >
                <i class="fas fa-pause-circle fa-lg"></i>
              </span>
            </li>

          </g:ifExecutionMode>
          <li id="appAdmin">
            <div class="dropdown">
              <a data-toggle="dropdown" class="dropdown-toggle">
                <i class="fas fa-cog fa-lg"></i>
              </a>
              <g:render template="/menu/sysConfigNavMenu"/>
            </div>
          </li>
          <li id="appUser">
            <div class="dropdown">
              <a data-toggle="dropdown" class="dropdown-toggle" id="userLabel">
                <i class="fas fa-user fa-lg"></i>
              </a>
              <g:render template="/menu/appUserMenu"/>
            </div>
          </li>
        </g:if>
      </ul>
    </div>
  </nav>
%{--  <g:javascript>--}%
%{--    jQuery(function(){--}%
%{--      jQuery('.navbar-minimize button, .navbar-minimize a.triangle').click(function(){--}%
%{--        jQuery('body').toggleClass('sidebar-mini');--}%
%{--        var sidebarOpen = localStorage.getItem('sidebarOpen')--}%
%{--        if(sidebarOpen === 'true'){--}%
%{--          localStorage.setItem('sidebarOpen', 'false')--}%
%{--        } else {--}%
%{--          localStorage.setItem('sidebarOpen', 'true')--}%
%{--        }--}%
%{--      });--}%
%{--      jQuery('button.navbar-toggle').click(function(e){--}%
%{--        jQuery('body').toggleClass('nav-open');--}%
%{--      });--}%
%{--    })--}%
%{--  </g:javascript>--}%
