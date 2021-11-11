%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

<%@ page import="org.rundeck.core.auth.AuthConstants" %>
<g:set var="authAdmin" value="${auth.resourceAllowedTest(
        action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
        type: AuthConstants.TYPE_PROJECT,
        name: (params.project ?: request.project),
        context: AuthConstants.CTX_APPLICATION
)}"/>
<g:set var="authDelete"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_DELETE,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION
       )}"/>
<g:set var="authExport"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_EXPORT,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION
       )}"/>
<g:set var="authImport"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_IMPORT,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION
       )}"/>
<g:set var="authConfigure"
       value="${authAdmin || auth.resourceAllowedTest(
               action: AuthConstants.ACTION_CONFIGURE,
               type: AuthConstants.TYPE_PROJECT,
               name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION
       )}"/>
<g:set var="authReadAcl"
       value="${auth.resourceAllowedTest(action: [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN],
               any: true,
               type: AuthConstants.TYPE_PROJECT_ACL,
               name: (params.project ?: request.project), context: AuthConstants.CTX_APPLICATION
       )}"/>

<g:set var="projectKeyStorageEnabled" value="${g.rConfig(value: "feature.projectKeyStorage.enabled", type: 'string') in [true,'true']}"/>

<script type="text/javascript">
    [
        {
            type: 'link',
            id: 'nav-project-settings-edit-project',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-cog',
            link: '${createLink(controller: "framework", action: "editProject", params: [project: params.project])}',
            label: '${g.message(code:"edit.configuration")}',
            active: false,
            enabled: ${authConfigure == true},
        },
        <g:if test="${projectKeyStorageEnabled}">
            {
                type: 'link',
                id: 'nav-project-settings-storage',
                container: 'nav-project-settings',
                group: 'main',
                class: 'fas fa-key',
                link: '${createLink(controller: "menu", action: "storage", params: [project: params.project])}',
                label: '${g.message(code:"gui.menu.KeyStorage")}',
                active: false,
                enabled: ${authConfigure == true},
            },
        </g:if>
        {
            type: 'link',
            id: 'nav-project-settings-edit-nodes',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-sitemap',
            link: '${createLink(controller: "framework", action: "projectNodeSources", params: [project: params.project])}',
            label: '${g.message(code:"edit.nodes")}',
            active: false,
            enabled: ${authConfigure == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-access-control',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-unlock-alt',
            link: '${createLink(controller: "menu", action: "projectAcls", params: [project: params.project])}',
            label: '${g.message(code:"gui.menu.AccessControl")}',
            active: false,
            enabled: ${authReadAcl == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-edit-readme',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-file-alt',
            link: '${raw(createLink(controller: "framework", action: "editProjectFile", params: [project: params.project, filename: 'readme.md']))}',
            label: '${g.message(code:"edit.readme.ellipsis")}',
            active: false,
            enabled: ${authConfigure == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-edit-motd',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-comment-alt',
            link: '${raw(createLink(controller: "framework", action: "editProjectFile", params: [project: params.project, filename: 'motd.md']))}',
            label: '${g.message(code:"edit.message.of.the.day")}',
            active: false,
            enabled: ${authConfigure == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-setup-scm',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-exchange-alt',
            link: '${createLink(controller: "scm", action: "index", params: [project: params.project])}',
            label: '${g.message(code:"project.admin.menu.Scm.title")}',
            active: false,
            enabled: ${authConfigure == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-export-archive',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-download',
            link: '${createLink(controller: "menu", action: "projectExport", params: [project: params.project])}',
            label: '${g.message(code:"export.archive.ellipsis")}',
            active: false,
            enabled: ${authExport == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-import-archive',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-upload',
            link: '${createLink(controller: "menu", action: "projectImport", params: [project: params.project])}',
            label: '${g.message(code:"import.archive.ellipsis")}',
            active: false,
            enabled: ${authImport == true},
        },
        {
            type: 'link',
            id: 'nav-project-settings-delete-project',
            container: 'nav-project-settings',
            group: 'main',
            class: 'fas fa-trash',
            link: '${createLink(controller: "menu", action: "projectDelete", params: [project: params.project])}',
            label: '${g.message(code:"delete.project.ellipsis")}',
            active: false,
            enabled: ${authExport == true},
        },
        <g:ifMenuItems type="PROJECT_CONFIG"  project="${params.project}">
            <g:forMenuItems type="PROJECT_CONFIG" var="item" project="${params.project}">
        {
            type: 'link',
            id: 'nav-${item.title.toLowerCase().replace(' ', '-')}-link',
            container: 'nav-project-settings',
            group: 'plugins',
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
    ].forEach(i => window._rundeck.navbar.items.push(i))
</script>
