<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
  <g:set var="selectParams" value="${[:]}"/>
  <g:if test="${pageScope._metaTabPage && pageScope._metaTabPage != 'configure'&& pageScope._metaTabPage != 'projectconfigure'}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
  </g:if>
  <nav id="mainbar" class="navbar navbar-default mainbar">
    <div class="container-fluid">
      <!-- <div class="navbar-minimize">
        <button class="btn btn-fill btn-icon">
          <i class="fas fa-ellipsis-v"></i>
          <i class="fas fa-ellipsis-h"></i>
        </button>
      </div> -->
      <div class="navbar-header">
        <button type="button" class="navbar-toggle">
          <span class="sr-only">Toggle navigation</span>
          <span class="icon-bar bar1"></span>
          <span class="icon-bar bar2"></span>
          <span class="icon-bar bar3"></span>
        </button>

        <g:set var="userDefinedLogo" value="${grailsApplication.config.rundeck?.gui?.logo}"/>
        <g:set var="userDefinedSmallLogo" value="${grailsApplication.config.rundeck?.gui?.logoSmall}"/>
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
                        <g:link controller="menu" action="projectHome" params="[project: projectName]">
                            <g:enc>${projectLabel}</g:enc>
                        </g:link>
                    </li>
                  <g:ifPageProperty name='meta.projtabtitle'>
                    <li class="primarylink">
                      <a href="#">
                        <g:pageProperty name="meta.projtabtitle"/>
                      </a>
                    </li>
                  </g:ifPageProperty>
                </g:if>
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
          </ul>
        </g:if>
      </div>

      <div class="collapse navbar-collapse">
          <g:if test="${(project?: params.project ?: request.project)}">
          <g:ifExecutionMode is="passive" project="${project?:params.project?:request.project}">
              <p class="navbar-text text-warning  has_tooltip" data-placement="right" title="${message(code:'project.execution.disabled')}">
                  <i class="glyphicon glyphicon-pause"></i>
              </p>
          </g:ifExecutionMode>
          <g:ifScheduleMode is="passive" project="${project?:params.project?:request.project}">
              <p class="navbar-text text-warning has_tooltip"  data-placement="right" title="${message(code:'project.schedule.disabled')}">
                  <i class="glyphicon glyphicon-ban-circle"></i>
              </p>
          </g:ifScheduleMode>
      </g:if>
        <ul id="navbar-menu" class="nav navbar-nav navbar-right">
          <g:set var="userDefinedInstanceName" value="${grailsApplication.config.rundeck?.gui?.instanceName}"/>
          <g:if test="${userDefinedInstanceName}">
            <li>
              <span class="label label-default instance-label" style="float:left;font-size: 20px;margin: 10px 15px 0 0;">
                  ${enc(sanitize:userDefinedInstanceName)}
              </span>
            </li>

          </g:if>


          <g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
            <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
              <g:if test="${grailsApplication.config.rundeck?.gui?.clusterIdentityInHeader in [true,'true']}">
                <li>
                  <span class="rundeck-server-uuid" data-server-uuid="${ servletContextAttribute(attribute: 'SERVER_UUID')}" data-server-name="${ servletContextAttribute(attribute: 'FRAMEWORK_NODE')}"></span>
                </li>
              </g:if>
            </g:ifServletContextAttribute>
          </g:ifServletContextAttributeExists>
          <g:if test="${session?.user && request.subject}">
            <g:ifExecutionMode passive="true">
              <li style="margin: 13px 20px 0 0;">
                <span class="has_tooltip"
                   title="${g.message(code:'passive.mode')} - ${g.message(code:'system.executionMode.description.passive')}"
                   data-toggle="tooltip"
                   data-placement="bottom"
                >
                  <i class="fas fa-pause-circle fa-2x"></i>
                </span>
              </li>

            </g:ifExecutionMode>
            <li id="appAdmin">
              <div class="dropdown" style="margin-top:3px;">
                <a data-toggle="dropdown" class="dropdown-toggle">
                  <i class="fas fa-cog fa-2x"></i>
                </a>
                <g:render template="/menu/sysConfigNavMenu"/>
              </div>
            </li>
            <li id="appUser">
              <div class="dropdown">
                <a data-toggle="dropdown" class="dropdown-toggle" id="userLabel">
                  <i class="fas fa-user"></i>
                </a>
                <g:render template="/menu/appUserMenu"/>
              </div>
            </li>
          </g:if>
        </ul>
      </div>
    </div>
  </nav>
  <g:javascript>
    jQuery(function(){
      jQuery('.navbar-minimize button, .navbar-minimize a.triangle').click(function(){
        jQuery('body').toggleClass('sidebar-mini');
        var sidebarOpen = localStorage.getItem('sidebarOpen')
        if(sidebarOpen === 'true'){
          localStorage.setItem('sidebarOpen', 'false')
        } else {
          localStorage.setItem('sidebarOpen', 'true')
        }
      });
      jQuery('button.navbar-toggle').click(function(e){
        jQuery('body').toggleClass('nav-open');
      });
    })
  </g:javascript>
