<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<g:set var="selectParams" value="${[:]}"/>
<g:if test="${pageScope._metaTabPage && pageScope._metaTabPage != 'configure'&& pageScope._metaTabPage != 'projectconfigure'}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
</g:if>
<nav class="navbar navbar-default">
  <div class="container-fluid">
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
                              <i class="glyphicon glyphicon-tasks"></i>
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
                      <li class="dropdown" id="projectSelect">
                          <g:render template="/framework/projectSelect"
                                    model="${[
                                            projects    : session.frameworkProjects,
                                            labels      : session.frameworkLabels,
                                            project     : params.project ?: request.project,
                                            selectParams: selectParams
                                    ]}"/>
                      </li>
                  </g:if>
                  <g:else>
                      <li id="projectSelect" class="dropdown disabled">
                          <a data-toggle="dropdown" href="#" class="disabled">
                              <i class="caret"></i>
                          </a>
                      </li>
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
                <bs:dropdownToggle css="toptab ${projconfigselected}">
                    <g:icon name="cog"/>
                </bs:dropdownToggle>
                <g:render template="/menu/sysConfigNavMenu"/>
            </li>
            <li>
                <bs:dropdownToggle id="userLabel" >
                    ${session.user}
                </bs:dropdownToggle>
                <bs:dropdown labelId="userLabel">
                    <bs:menuitem
                            controller="user" action="profile"
                            icon="user"
                            code="profile"/>

                    <bs:menuitem/>
                    <bs:menuitem action="logout" controller="user"
                                 icon="remove"
                                 code="logout"/>
                </bs:dropdown>
            </li>
        </g:if>
      </ul>
    </div>
  </div>
</nav>
