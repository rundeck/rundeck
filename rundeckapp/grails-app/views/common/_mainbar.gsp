<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:set var="selectParams" value="${[:]}"/>
<g:if test="${pageScope._metaTabPage && pageScope._metaTabPage != 'configure'&& pageScope._metaTabPage != 'projectconfigure'}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
</g:if>
<nav class="navbar navbar-default mainbar">
  <div class="container-fluid">
    <div class="navbar-minimize">
      <button class="btn btn-fill btn-icon">
        <i class="fas fa-ellipsis-v"></i>
        <i class="fas fa-ellipsis-h"></i>
      </button>
    </div>
    <div class="navbar-header">
      <button type="button" class="navbar-toggle">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar bar1"></span>
        <span class="icon-bar bar2"></span>
        <span class="icon-bar bar3"></span>
      </button>
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
                  <g:if test="${params.project ?: request.project}">

                      <li id="projectHomeLink">
                          <a href="${createLink(
                                  controller: 'menu',
                                  action: 'projectHome',
                                  params: [project: project ?: params.project ?: request.project]
                          )}">
                              <g:if test="${session.frameworkLabels}">
                                  <g:enc>${project ?session.frameworkLabels[project]: params.project ?
                                          session.frameworkLabels[params.project]: request.project ?
                                          session.frameworkLabels[request.project]: 'Choose ...'}</g:enc>
                              </g:if>
                              <g:if test="${!session.frameworkLabels}">
                                  <g:enc>${project ?: params.project ?: request.project ?: 'Choose ...'}</g:enc>
                              </g:if>
                          </a>
                      </li>
                  </g:if>
                  <g:if test="${request.getAttribute(RequestConstants.PAGE)}">
                      <g:ifPageProperty name='meta.tabpage'>
                          <g:ifPageProperty name='meta.tabpage' equals='projectconfigure'>
                              <g:set var="projconfigselected" value="active"/>
                          </g:ifPageProperty>
                      </g:ifPageProperty>
                  </g:if>
                  <g:if test="${session.frameworkProjects}">
                    %{--
                      Removed and placed in the sidebar
                      <li id="projectSelect">
                          <g:render template="/framework/projectSelect"
                                    model="${[
                                            projects    : session.frameworkProjects,
                                            labels      : session.frameworkLabels,
                                            project     : params.project ?: request.project,
                                            selectParams: selectParams
                                    ]}"/>
                      </li>
                    --}%
                  </g:if>
                  <g:else>
                      <!-- There's no reason for an empty dropdown -->
                  </g:else>
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
          </ul>
      </g:if>
    </div>
    <div class="collapse navbar-collapse">
      <ul class="nav navbar-nav navbar-right">
        <g:ifServletContextAttributeExists attribute="CLUSTER_MODE_ENABLED">
            <g:ifServletContextAttribute attribute="CLUSTER_MODE_ENABLED" value="true">
                <g:if test="${grailsApplication.config.rundeck?.gui?.clusterIdentityInHeader in [true,'true']}">
                    <li>
                        <span class="rundeck-server-uuid"
                              data-server-uuid="${ servletContextAttribute(attribute: 'SERVER_UUID')}"
                              data-server-name="${ servletContextAttribute(attribute: 'FRAMEWORK_NODE')}"
                        >
                        </span>
                    </li>
                </g:if>
            </g:ifServletContextAttribute>
        </g:ifServletContextAttributeExists>
        <g:if test="${session?.user && request.subject}">
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
                  ${session.user}
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
    jQuery('.navbar-minimize button').click(function(){
      jQuery('body').toggleClass('sidebar-mini');
    });

  })
</g:javascript>
