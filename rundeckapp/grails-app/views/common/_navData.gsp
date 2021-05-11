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
                <g:if test="${projectName}">
                {
                    type: 'link',
                    id: 'nav-project-dashboard-link',
                    group: 'main',
                    class: 'fas fa-clipboard-list',
                    link: '${createLink(controller: "menu", action: "projectHome", params: [project: project ?: projectName])}',
                    label: '${g.message(code:"gui.menu.Dashboard")}',
                    active: ${homeselected == 'active'},
                },
                {
                    type: 'link',
                    id: 'nav-jobs-link',
                    group: 'main',
                    class: 'fas fa-tasks',
                    link: '${createLink(controller: "menu", action: "jobs", params: [project: projectName])}',
                    label: '${g.message(code: "gui.menu.Workflows")}',
                    active: ${wfselected == 'active'},
                },
                {
                    type: 'link',
                    id: 'nav-nodes-link',
                    group: 'main',
                    class: 'fas fa-sitemap',
                    link: '${createLink(controller: "framework", action: "nodes", params: [project: projectName])}',
                    label: '${g.message(code: "gui.menu.Nodes")}',
                    active: ${resselected == 'active'},
                },

                <g:if test="${auth.adhocAllowedTest(action: AuthConstants.ACTION_RUN, project: projectName)}">
                {
                    type: 'link',
                    id: 'nav-commands-link',
                    group: 'main',
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
                    group: 'main',
                    class: 'fas fa-history',
                    link: '${createLink(controller: "reports", action: "index", params: [project: project ?: projectName])}',
                    label: '${g.message(code: "gui.menu.Events")}',
                    active: ${eventsselected == 'active'},
                },
                </auth:resourceAllowed>


                    <g:if test="${params.project ?: request.project}">
                        <g:ifMenuItems type="PROJECT" project="${projectName}">
                            <g:forMenuItems type="PROJECT" var="item" project="${projectName}">
                {
                    type: 'link',
                    id: 'nav-${item.title.toLowerCase().replace(' ', '-')}-link',
                    group: 'main',
                    priority: '${enc(attr: item.priority)}',
                    class: '${enc(attr: item.iconCSS ?: 'fas fa-plug')}',
                    link: '${enc(attr: item.getProjectHref(projectName))}',
                    label: '${g.message(code: item.titleCode, default: item.title)}',
                    <g:ifPageProperty name='meta.tabpage'>
                    <g:ifPageProperty name='meta.tabpage' equals='${item.title}'>
                    active: true
                    </g:ifPageProperty>
                    </g:ifPageProperty>
                },
                            </g:forMenuItems>
                        </g:ifMenuItems>
                    </g:if>
                <g:if test="${projConfigAuth||projACLAuth}">
                {
                    id: 'nav-project-settings',
                    type: 'container',
                    style: 'list',
                    group: 'bottom',
                    class: 'fas fa-cogs',
                    label: '${g.message(code: "gui.menu.ProjectSettings")}'
                }
                </g:if>
                </g:if>
            ]
        }
    });
</script>

<g:if test="${projectName}">
    <g:render template="/common/navBarProjectSettingsData"/>
</g:if>
