<%@ page import="com.opensymphony.module.sitemesh.RequestConstants; org.rundeck.core.auth.AuthConstants" %>

<g:set var="selectedclass" value="active"/>
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

<g:set var="projectName" value="${params.project ?: request.project}"/>

<g:if test="${projectName}">
    <g:set var="projConfigAuth"
           value="${auth.resourceAllowedTest(
                   type: AuthConstants.TYPE_PROJECT,
                   name: (projectName),
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
                   name: (projectName),
                   action: [AuthConstants.ACTION_READ,
                            AuthConstants.ACTION_ADMIN],
                   any: true,
                   context: 'application'
           )}"/>
</g:if>

<script type="text/javascript">
    window._rundeck = Object.assign(window._rundeck || {}, {
        navbar: {
            items: [
                {
                    "type": "link",
                    "id": "nav-rd-home",
                    "class": "rdicon app-logo",
                    "link": "/",
                    "label": "",
                },
                <g:if test="${projectName}">
                {
                    type: 'link',
                    id: 'nav-project-dashboard-link',
                    class: 'fas fa-clipboard-list',
                    link: '${createLink(controller: "menu", action: "projectHome", params: [project: project ?: projectName])}',
                    label: '${g.message(code:"gui.menu.Dashboard")}',
                    active: ${homeselected == 'active'},
                },
                {
                    type: 'link',
                    id: 'nav-jobs-link',
                    class: 'fas fa-tasks',
                    link: '${createLink(controller: "menu", action: "jobs", params: [project: projectName])}',
                    label: '${g.message(code: "gui.menu.Workflows")}',
                    active: ${wfselected == 'active'},
                },
                {
                    type: 'link',
                    id: 'nav-nodes-link',
                    class: 'fas fa-sitemap',
                    link: '${createLink(controller: "framework", action: "nodes", params: [project: projectName])}',
                    label: '${g.message(code: "gui.menu.Nodes")}',
                    active: ${resselected == 'active'},
                },

                <g:if test="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN, project: projectName)}">
                {
                    type: 'link',
                    id: 'nav-commands-link',
                    class: 'fas fa-terminal',
                    link: '${createLink(controller: "framework", action: "adhoc", params: [project: projectName])}',
                    label: '${g.message(code: "gui.menu.Adhoc")}',
                    active: ${adhocselected == 'active'},
                },
                </g:if>


                <auth:resourceAllowed project="${projectName}" action="${[AuthConstants.ACTION_READ]}" kind="event">
                {
                    type: 'link',
                    id: 'nav-activity-link',
                    class: 'fas fa-history',
                    link: '${createLink(controller: "reports", action: "index", params: [project: project ?: projectName])}',
                    label: '${g.message(code: "gui.menu.Events")}',
                    active: ${eventsselected == 'active'},
                },
                </auth:resourceAllowed>


                    <g:if test="${params.project ?: request.project}">
                        <g:ifMenuItems type="PROJECT" project="${projectName}">
                %{--            <li role="separator" class="divider"></li>--}%
                            <g:forMenuItems type="PROJECT" var="item" project="${projectName}">
                %{--                <li>--}%
                %{--                    <a href="${enc(attr: item.getProjectHref(projectName))}"--}%
                %{--                       class=" toptab "--}%
                %{--                       title="${enc(attr: g.message(code: item.titleCode, default: item.title))}">--}%
                %{--                        <i class="${enc(attr: item.iconCSS ?: 'fas fa-plug')}"></i>--}%
                %{--                        <p><g:message code="${item.titleCode}" default="${item.title}"/></p>--}%
                %{--                    </a>--}%
                %{--                </li>--}%
                {
                    type: 'link',
                    id: '',
                    class: '${enc(attr: item.iconCSS ?: 'fas fa-plug')}',
                    link: '${enc(attr: item.getProjectHref(projectName))}',
                    label: '${g.message(code: item.titleCode, default: item.title)}'
                },
                            </g:forMenuItems>
                        </g:ifMenuItems>
                    </g:if>
                <g:if test="${projConfigAuth||projACLAuth}">
                {
                    type: 'container',
                    id: 'nav-project-settings',
                    class: 'fas fa-cogs',
                    label: '${g.message(code: "gui.menu.ProjectSettings")}'
                }
%{--                <g:render template="navBarProjectSettingsData"/>--}%
                </g:if>
                </g:if>
            ]
        }
    });
    console.log(window._rundeck);
</script>

%{--        --}%
%{--        <g:if test="${session.frameworkProjects}">--}%
%{--            <li id="projectSelect">--}%
%{--                <a href="#" data-toggle="collapse">--}%
%{--                    <i class="fas fa-suitcase"></i>--}%
%{--                    <p>--}%
%{--                        <g:message code="gui.menu.Projects"/>--}%
%{--                        <b class="caret"></b>--}%
%{--                    </p>--}%
%{--                </a>--}%
%{--                <g:render template="/framework/projectSelectSidebar"--}%
%{--                          model="${[--}%
%{--                                  projects    : session.frameworkProjects,--}%
%{--                                  labels      : session.frameworkLabels,--}%
%{--                                  project     : projectName,--}%
%{--                                  selectParams: selectParams--}%
%{--                          ]}"/>--}%
%{--            </li>--}%
%{--        </g:if>--}%
%{--    </feature:enabled>--}%
%{--<g:if test="${projectName}">--}%
%{--    <li id="nav-project-dashboard-link" class="${enc(attr: homeselected)}">--}%
%{--        <g:link controller="menu" action="projectHome" params="[project: project ?: projectName]">--}%
%{--            <i class="fas fa-clipboard-list"></i>--}%
%{--            <p>--}%
%{--                <g:message code="gui.menu.Dashboard"/>--}%
%{--            </p>--}%
%{--        </g:link>--}%
%{--    </li>--}%
%{--    <li id="nav-jobs-link" class="${enc(attr: wfselected)}">--}%
%{--        <g:link controller="menu" action="jobs" class=" toptab ${enc(attr: wfselected)}" params="[project: projectName]">--}%
%{--            <i class="fas fa-tasks"></i>--}%
%{--            <p>--}%
%{--                <g:message code="gui.menu.Workflows"/>--}%
%{--            </p>--}%
%{--        </g:link>--}%
%{--    </li>--}%
%{--    <li id="nav-nodes-link" class="${enc(attr:resselected)}">--}%
%{--        <g:link controller="framework" action="nodes" class=" toptab ${enc(attr: resselected)}" params="[project: projectName]">--}%
%{--            <i class="fas fa-sitemap"></i>--}%
%{--            <p>--}%
%{--                <g:message code="gui.menu.Nodes"/>--}%
%{--            </p>--}%
%{--        </g:link>--}%
%{--    </li>--}%
%{--    <g:if test="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN, project: projectName)}">--}%
%{--        <li id="nav-commands-link" class="${enc(attr: adhocselected)}">--}%
%{--            <g:link controller="framework" action="adhoc" class=" toptab ${enc(attr: adhocselected)}" params="[project: projectName]">--}%
%{--                <i class="fas fa-terminal"></i>--}%
%{--                <p>--}%
%{--                    <g:message code="gui.menu.Adhoc"/>--}%
%{--                </p>--}%
%{--            </g:link>--}%
%{--        </li>--}%
%{--    </g:if>--}%
%{--    <auth:resourceAllowed project="${projectName}" action="${[AuthConstants.ACTION_READ]}" kind="event">--}%
%{--        <li id="nav-activity-link" class="${enc(attr: eventsselected)}">--}%
%{--            <g:link controller="reports" action="index" class=" toptab ${enc(attr: eventsselected)}" params="[project: projectName]">--}%
%{--                <i class="fas fa-history"></i>--}%
%{--                <p>--}%
%{--                    <g:message code="gui.menu.Events"/>--}%
%{--                </p>--}%
%{--            </g:link>--}%
%{--        </li>--}%
%{--    </auth:resourceAllowed>--}%
%{--    <g:if test="${params.project ?: request.project}">--}%
%{--        <g:ifMenuItems type="PROJECT" project="${projectName}">--}%
%{--            <li role="separator" class="divider"></li>--}%
%{--            <g:forMenuItems type="PROJECT" var="item" project="${projectName}">--}%
%{--                <li>--}%
%{--                    <a href="${enc(attr: item.getProjectHref(projectName))}"--}%
%{--                       class=" toptab "--}%
%{--                       title="${enc(attr: g.message(code: item.titleCode, default: item.title))}">--}%
%{--                        <i class="${enc(attr: item.iconCSS ?: 'fas fa-plug')}"></i>--}%
%{--                        <p><g:message code="${item.titleCode}" default="${item.title}"/></p>--}%
%{--                    </a>--}%
%{--                </li>--}%
%{--            </g:forMenuItems>--}%
%{--        </g:ifMenuItems>--}%
%{--    </g:if>--}%
%{--    <g:set var="projConfigAuth"--}%
%{--           value="${auth.resourceAllowedTest(--}%
%{--                   type: AuthConstants.TYPE_PROJECT,--}%
%{--                   name: (projectName),--}%
%{--                   action: [AuthConstants.ACTION_CONFIGURE,--}%
%{--                            AuthConstants.ACTION_ADMIN,--}%
%{--                            AuthConstants.ACTION_IMPORT,--}%
%{--                            AuthConstants.ACTION_EXPORT,--}%
%{--                            AuthConstants.ACTION_DELETE],--}%
%{--                   any: true,--}%
%{--                   context: 'application'--}%
%{--           )}"/>--}%
%{--    <g:set var="projACLAuth"--}%
%{--           value="${auth.resourceAllowedTest(--}%
%{--                   type: AuthConstants.TYPE_PROJECT_ACL,--}%
%{--                   name: (projectName),--}%
%{--                   action: [AuthConstants.ACTION_READ,--}%
%{--                            AuthConstants.ACTION_ADMIN],--}%
%{--                   any: true,--}%
%{--                   context: 'application'--}%
%{--           )}"/>--}%

%{--    <g:if test="${projConfigAuth||projACLAuth}">--}%

%{--        <g:ifPageProperty name='meta.projconfigselected'>--}%
%{--            <script type="text/javascript">--}%
%{--                jQuery(function () {--}%
%{--                    jQuery('#nav-project-settings-${enc(js:g.pageProperty(name:"meta.projconfigselected"))}').addClass(--}%
%{--                        'active');--}%
%{--                })--}%
%{--            </script>--}%
%{--            <g:set var="projConfigOpen" value="${true}"/>--}%
%{--        </g:ifPageProperty>--}%

%{--        <li id="nav-project-settings">--}%
%{--            <a href="#" data-toggle="collapse" class="${wdgt.css(if: projConfigOpen, then: 'subnav-open')}">--}%
%{--                <i class="fas fa-cogs"></i>--}%
%{--                <p>--}%
%{--                    <g:message code="gui.menu.ProjectSettings"/>--}%
%{--                    <b class="caret"></b>--}%
%{--                </p>--}%
%{--            </a>--}%
%{--            <g:render template="/menu/sidebarProjectMenu" model="[projConfigOpen: projConfigOpen]"/>--}%
%{--        </li>--}%