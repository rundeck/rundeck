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
<g:set var="buildIdent" value="${servletContextAttribute(attribute: 'app.ident')}"/>
<g:set var="appId" value="${g.appTitle()}"/>
<g:if test="${pageScope._metaTabPage && !(pageScope._metaTabPage in ['configure','projectconfigure','home'])}">
    <g:set var="selectParams" value="${[page: _metaTabPage,project:params.project?:request.project]}"/>
</g:if>



<g:set var="selectedclass" value="active"/>

<g:set var="wfselected" value=""/>
<ul id="sidebar-nav" class="nav">

<g:if test="${request.getAttribute(RequestConstants.PAGE)}">
    <g:ifPageProperty name='meta.tabpage'>
        <g:ifPageProperty name='meta.tabpage' equals='projectHome'>
            <g:set var="homeselected" value="${selectedclass}"/>
        </g:ifPageProperty>
    </g:ifPageProperty>
    <g:ifPageProperty name='meta.tabpage'>
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
<g:if test="${session?.user && request.subject }">
<g:if test="${session.frameworkProjects}">
    <li id="projectSelect">
      <a href="#" data-toggle="collapse">
        <i class="fas fa-suitcase"></i>
        <p>
          <g:message code="gui.menu.Projects"/>
          <b class="caret"></b>
        </p>
      </a>
        <g:render template="/framework/projectSelectSidebar"
                  model="${[
                          projects    : session.frameworkProjects,
                          labels      : session.frameworkLabels,
                          project     : params.project ?: request.project,
                          selectParams: selectParams
                  ]}"/>
    </li>
</g:if>
<g:if test="${params.project ?: request.project}">
    <li id="nav-project-dashboard-link" class="${enc(attr: homeselected)}">
      <g:link controller="menu" action="projectHome" params="[project: project ?: params.project ?: request.project]">
      <i class="fas fa-clipboard-list"></i>
        <p>
          <g:message code="gui.menu.Dashboard"/>
        </p>
      </g:link>
    </li>
    <li id="nav-jobs-link" class="${enc(attr: wfselected)}">
        <g:link controller="menu" action="jobs" class=" toptab ${enc(attr: wfselected)}" params="[project: params.project ?: request.project]">
            <i class="fas fa-tasks"></i>
            <p>
              <g:message code="gui.menu.Workflows"/>
            </p>
        </g:link>
    </li>
    <li id="nav-nodes-link" class="${enc(attr:resselected)}">
        <g:link controller="framework" action="nodes" class=" toptab ${enc(attr: resselected)}" params="[project: params.project ?: request.project]">
            <i class="fas fa-sitemap"></i>
            <p>
              <g:message code="gui.menu.Nodes"/>
            </p>
        </g:link>
    </li>
    <g:if test="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN, project: params.project ?: request.project)}">
        <li id="nav-commands-link" class="${enc(attr: adhocselected)}">
            <g:link controller="framework" action="adhoc" class=" toptab ${enc(attr: adhocselected)}" params="[project: params.project ?: request.project]">
                <i class="fas fa-terminal"></i>
                <p>
                  <g:message code="gui.menu.Adhoc"/>
                </p>
            </g:link>
        </li>
    </g:if>
    <li id="nav-activity-link" class="${enc(attr: eventsselected)}">
      <g:link controller="reports" action="index" class=" toptab ${enc(attr: eventsselected)}" params="[project: params.project ?: request.project]">
        <i class="fas fa-history"></i>
        <p>
          <g:message code="gui.menu.Events"/>
        </p>
      </g:link>
    </li>
    <g:ifMenuItems type="PROJECT" project="${params.project}">
        <li role="separator" class="divider"></li>
        <g:forMenuItems type="PROJECT" var="item" project="${params.project}">
            <li>
                <a href="${enc(attr: item.getProjectHref(params.project))}"
                   class=" toptab "
                   title="${enc(attr: g.message(code: item.titleCode, default: item.title))}">
                    <i class="${enc(attr: item.iconCSS ?: 'fas fa-plug')}"></i>
                    <p><g:message code="${item.titleCode}" default="${item.title}"/></p>
                </a>
            </li>
        </g:forMenuItems>
    </g:ifMenuItems>
    <g:set var="projConfigAuth"
           value="${auth.resourceAllowedTest(
                   type: AuthConstants.TYPE_PROJECT,
                   name: (params.project ?: request.project),
                   action: [AuthConstants.ACTION_CONFIGURE,
                            AuthConstants.ACTION_ADMIN,
                            AuthConstants.ACTION_IMPORT,
                            AuthConstants.ACTION_EXPORT,
                            AuthConstants.ACTION_DELETE],
                   any: true,
                   context: 'application'
           )}"/>
    <g:set var="projACLAuth"
           value="${auth.resourceAllowedTest(
                   type: AuthConstants.TYPE_PROJECT_ACL,
                   name: (params.project ?: request.project),
                   action: [AuthConstants.ACTION_READ,
                            AuthConstants.ACTION_ADMIN],
                   any: true,
                   context: 'application'
           )}"/>

    <g:if test="${projConfigAuth||projACLAuth}">

        <g:ifPageProperty name='meta.projconfigselected'>
            <script type="text/javascript">
                jQuery(function () {
                    jQuery('#nav-project-settings-${enc(js:g.pageProperty(name:"meta.projconfigselected"))}').addClass(
                        'active');
                })
            </script>
            <g:set var="projConfigOpen" value="${true}"/>
        </g:ifPageProperty>

        <li id="nav-project-settings">
            <a href="#" data-toggle="collapse" class="${wdgt.css(if: projConfigOpen, then: 'subnav-open')}">
            <i class="fas fa-cogs"></i>
            <p>
              <g:message code="gui.menu.ProjectSettings"/>
              <b class="caret"></b>
            </p>
          </a>
            <g:render template="/menu/sidebarProjectMenu" model="[projConfigOpen: projConfigOpen]"/>
        </li>
    </g:if>
</g:if>
</g:if>
</ul>
<g:if test="${request.getAttribute(RequestConstants.PAGE)}">
    <g:ifPageProperty name='meta.tabtitle'>
        <ul class="nav">
            <li class="active">
                <a href="">
                    <g:icon name="menu-right"/>
                    <p>
                      <g:pageProperty name='meta.tabtitle'/>
                    </p>
                </a>
            </li>
        </ul>
    </g:ifPageProperty>
</g:if>
<div id="sidebar-bottom" style="border-top: 1px solid #3c3c3c;">
  <div id="community-news-notification">
    <div class="sidebar-footer-line-item">
      <g:if test="${grailsApplication.config.rundeck.communityNews.disabled in [true,'true']}">
        <a href="https://www.rundeck.com/community-updates" target="_blank">
          <div>
            <i class="far fa-newspaper" style="margin-right:5px;"></i>
            <span>Community News</span>
          </div>
        </a>
      </g:if>
      <g:else>
        <g:link controller="communityNews" action="index">
          <span id="community-news-notification-vue"></span>
        </g:link>
      </g:else>
    </div>
  </div>
  <div id="version-notification-vue"></div>
  <div id="snapshot-version" class="snapshot-version">
    <span class="rundeck-version-identity"
          data-version-string="${enc(attr: buildIdent)}"
          data-version-date="${enc(attr: servletContextAttribute(attribute: 'version.date_short'))}"
          data-app-id="${enc(attr: appId)}"
          style="display:block;"></span>
    <g:link controller="menu" action="welcome" class="version link-bare">
        <g:appTitle/> ${buildIdent}
    </g:link>
  </div>
</div>

<g:javascript>


  jQuery(function(){
    // Sets user preference on opening/closing the sidebar
    jQuery('.navbar-minimize a').click(function(){

      var key = 'sidebarClosed'
      var sidebarClosed = jQuery('body').hasClass('sidebar-mini')

      sidebarClosed = !sidebarClosed // if the sidebar has that class, we're flipping it for the save

       jQuery.ajax({
            url: _genUrl(appLinks.userAddFilterPref, {filterpref: key + "=" + sidebarClosed}),
            method: 'POST',
            beforeSend: _ajaxSendTokens.curry('ui_token'),
            success: function () {
                console.log("saved sidebar position" );
            },
            error: function () {
                console.log("saving sidebar position failed" );
            }
        })
        .success(_ajaxReceiveTokens.curry('ui_token'));
    })
    // Mobile Sidebar
    jQuery('.sidebar-wrapper a[data-toggle="collapse"]').click(function(){
      jQuery(this).next().slideToggle();
      jQuery(this).toggleClass('subnav-open');
    });
  })
  // Sidebar - Perfect Sidebar Scroller
  const ps = new PerfectScrollbar('.sidebar-wrapper', {
    suppressScrollX: true
  });

  setTimeout(function(){
    var announcementHeight = document.getElementById("sidebar-bottom").offsetHeight;
    document.getElementById("sidebar-nav").style['marginBottom'] = announcementHeight.toString() + "px";
  }, 500)

</g:javascript>
