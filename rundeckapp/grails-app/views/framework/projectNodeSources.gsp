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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/6/17
  Time: 9:39 AM
--%>

<%@ page import="com.dtolabs.rundeck.core.common.FrameworkProject" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <g:set var="rkey" value="${g.rkey()}"/>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="base"/>
  <meta name="tabpage" content="projectconfigure"/>
  <meta name="projtabtitle" content="${message(code: 'edit.nodes.title')}"/>
  <meta name="projconfigselected" content="edit-nodes"/>
  <meta name="skipPrototypeJs" content="true"/>

  <title><g:message code="edit.nodes.title"/>: <g:enc>${params.project ?: request.project}</g:enc></title>

  <!-- VUE JS MODULES -->
  <asset:javascript src="static/pages/project-nodes-config.js" defer="defer"/>
  <asset:stylesheet href="static/css/pages/project-nodes-config.css"/>
  <!-- /VUE JS MODULES -->
</head>

<body>
<div class="container-fluid">
  <div class="row">
    <div class="col-xs-12">
      <g:render template="/common/messages"/>
    </div>
  </div>

  <div class="row row-space-bottom">
    <div class="col-xs-12">
      <div class="" id="createform">
        <div class=" vue-tabs">

          <ul class="nav nav-pills" style="display: inline-block">

            <g:if test="${!legacyProjectNodesUi && feature.isDisabled(name: 'legacyProjectNodesUi')}">

              <li class="${writeableSources ? 'active' : ''}" id="tab_link_sources_writeable">
                <a href="#node_sources_writeable" data-toggle="tab">
                  <i class="fas fa-pencil-alt "></i>
                  <g:message code="button.Edit.label"/>
                </a>
              </li>

              <li class="${writeableSources ? '' : 'active'}">
                <a href="#node_sources" data-toggle="tab" id="tab_link_sources">
                  <i class="fas fa-hdd"></i>
                  <g:message code="project.node.sources.title.short"/>
                </a>
              </li>
            </g:if>

            <feature:enabled name="enhanced-nodes">

              <li id="tab_link_plugins">
                <a href="#node_plugins" data-toggle="tab">
                  <i class="fas fa-puzzle-piece"></i>
                  <g:message code="framework.service.NodeEnhancer.label.short.plural"/>
                </a>
              </li>

            </feature:enabled>

            <li id="tab_link_settings" class="${legacyProjectNodesUi || feature.isEnabled(name: 'legacyProjectNodesUi') ? 'active':''}">
              <a href="#node_settings" data-toggle="tab">
                <i class="fas fa-cog"></i>
                <g:message code="configuration"/>
              </a>
            </li>
          </ul>


          %{--Shared page-confirm handler--}%
          <page-confirm :event-bus="EventBus"
                        class="project-plugin-config-vue pull-right"
                        message="${enc(attr:message(code:'page.unsaved.changes'))}"
                        :display="true" style="display: inline-block">
            <div class="well well-sm" slot="default" slot-scope="{confirm}">
              <span class="text-warning">
                <g:message code="page.unsaved.changes"/>:
              </span>

              <span v-if="confirm.indexOf('Node Sources')>=0">
                <a href="#node_sources" onclick="jQuery('#tab_link_sources').tab('show')">
                  <i class="fas fa-hdd fa-edit"></i>
                  <g:message code="project.node.sources.title.short"/>
                </a>
              </span>
              <span v-if="confirm.indexOf('Node Enhancers')>=0">
                <a href="#node_plugins" onclick="jQuery('#tab_link_plugins').tab('show')">
                  <i class="fas fa-puzzle-piece"></i>
                  <g:message code="framework.service.NodeEnhancer.label.short.plural"/>
                </a>
              </span>
            </div>
          </page-confirm>
        </div>
      </div>
    </div>
  </div>

  <div class="row row-space-lg">
    <div class="col-xs-12">
      <div class="tab-content">
        <div class="tab-pane ${legacyProjectNodesUi || feature.isEnabled(name: 'legacyProjectNodesUi') ? 'active':''}" id="node_settings">

          <g:if test="${resourceModelConfigDescriptions && (legacyProjectNodesUi || feature.isEnabled(name: 'legacyProjectNodesUi'))}">
          %{--NOTE: Legacy UI--}%

            <div class="card">
              <div class="card-content">
                <div class="panel panel-default">
                  <div class="panel-heading">

                    <g:link controller="framework" action="editProjectNodeSources"
                            params="[project: params.project ?: request.project]"
                            class=" btn btn-info btn-sm">
                      <g:icon name="pencil"/>
                      <g:message code="edit.configuration"/>
                    </g:link>
                  </div>

                  <div class="panel-body">
                    <g:render template="/menu/projectConfigurableView"
                              model="${[extraConfigSet: extraConfig?.values(),
                                        category      : 'resourceModelSource',
                                        titleCode     : 'project.configuration.extra.category.resourceModelSource.title',
                                        helpCode      : 'project.configuration.extra.category.resourceModelSource.description'
                              ]}"/>
                    </div>
                </div>

                <g:render template="legacyNodeSourcesList"/>

              </div>
            </div>
          %{--END: Legacy UI--}%
          </g:if>
          <g:else>
          %{--updated UI--}%

            <g:form action="saveProjectNodeSources" method="post" useToken="true" class="form">
              <div class="card">
                <div class="card-content">


                    <g:hiddenField name="project" value="${project}"/>
                    <%--Render project configuration settings for 'resourceModelSource'--%>
                    <g:render template="projectConfigurableForm"
                              model="${[extraConfigSet: extraConfig?.values(),
                                        category      : 'resourceModelSource',
                                        categoryPrefix: 'extra.category.resourceModelSource.',

                                        helpCode      : 'project.configuration.extra.category.resourceModelSource.description'
                              ]}"/>



                </div>

                <div class="card-footer">

                  <g:submitButton name="save" value="${g.message(code: 'button.action.Save', default: 'Save')}"
                                  class="btn btn-primary reset_page_confirm"/>
                </div>
              </div>
            </g:form>
          </g:else>

        </div>

        <g:if test="${!legacyProjectNodesUi && feature.isDisabled(name: 'legacyProjectNodesUi')}">

          <div class="tab-pane ${writeableSources ? 'active' : ''} project-plugin-config-vue"
               id="node_sources_writeable">

            <writeable-project-node-sources item-css="card"
                                            item-content-css="card-content"
                                            :event-bus="EventBus">
              <div slot="empty" class="card">


                <div class="card-content">
                  <span class="text-info"><i class="glyphicon glyphicon-info-sign"></i> No modifiable sources found</span>
                </div>


              </div>
            </writeable-project-node-sources>

            <div class="card">
              <div class="card-header">
                <span class="help-block"><g:message code="modifiable.node.sources.will.appear.here" /></span>
              </div>

              <div class="card-footer">
                <div class="well well-sm">
                  <g:message code="use.the.node.sources.tab.1" />
                  <a href="#node_sources" onclick="jQuery('#tab_link_sources').tab('show')">
                    <i class="fas fa-hdd fa-edit"></i>
                    <g:message code="project.node.sources.title.short"/>
                  </a>
                  <g:message code="use.the.node.sources.tab.2" />
                </div>
              </div>
            </div>
          </div>


          <div class="tab-pane ${writeableSources ? '' : 'active'}" id="node_sources">

            <div class="card">
              <div class="card-content">
                <project-node-sources-config class="project-plugin-config-vue"
                                             help="${enc(attr: g.message(code: "domain.Project.edit.ResourceModelSource.explanation"))}"
                                             :edit-mode="true"
                                             :mode-toggle="false"
                                             @saved="EventBus.$emit('project-node-sources-saved')"
                                             @modified="EventBus.$emit('page-modified','Node Sources')"
                                             @reset="EventBus.$emit('page-reset','Node Sources')"
                                             :event-bus="EventBus">
                </project-node-sources-config>

              </div>
            </div>
          </div>
        </g:if>

        <feature:enabled name="enhanced-nodes">
          <div class="tab-pane" id="node_plugins">

            <div class="card">
              <div class="card-content">
                <project-plugin-config class="project-plugin-config-vue"
                                       config-prefix="nodes.plugin"
                                       service-name="NodeEnhancer"
                                       help="${enc(attr: g.message(code: 'framework.service.NodeEnhancer.explanation'))}"
                                       edit-button-text="${enc(attr: g.message(code: 'edit.node.enhancers'))}"
                                       :mode-toggle="false"
                                       @modified="EventBus.$emit('page-modified','Node Enhancers')"
                                       @reset="EventBus.$emit('page-reset','Node Enhancers')"
                                       :event-bus="EventBus"
                                       :edit-mode="true">

                </project-plugin-config>
              </div>
            </div>
          </div>
        </feature:enabled>

      </div>
    </div>
    </div>
  </div>

</body>
</html>
