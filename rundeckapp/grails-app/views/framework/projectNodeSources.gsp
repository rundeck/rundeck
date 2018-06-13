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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="projectconfigure"/>
    <meta name="projtabtitle" content="${message(code: 'Node.plural')}"/>
    <title><g:message code="Node.plural"/></title>

</head>

<body>
<div class="container-fluid">
  <div class="row">
    <div class="col-xs-12">
      <g:render template="/common/messages"/>
    </div>
  </div>
  <div class="row">
    <g:form action="saveProject" method="post" useToken="true" onsubmit="return configControl.checkForm();" class="form">
      <div class="col-xs-12">
        <div class="card"  id="createform">
          <div class="card-header">
            <h3 class="card-title">
              <g:message code="Node.plural" default="Nodes"/>: <g:enc>${params.project ?: request.project}</g:enc>
              <g:link controller="framework" action="editProjectNodeSources"
                      params="[project: params.project ?: request.project]"
                      class="has_tooltip pull-right btn btn-default btn-xs"
                      data-placement="bottom"
                      title="${message(
                            code: 'project.configure.nodes.title',
                )}">
                <g:icon name="pencil"/>
                <g:message code="project.configure.nodes.title"/>
              </g:link>
            </h3>
          </div>
          <div class="card-content">
            <div class="list-group">
              <g:hiddenField name="project" value="${project}"/>
                <g:render template="/menu/projectConfigurableView"
                          model="${[extraConfigSet: extraConfig?.values(),
                                    category      : 'resourceModelSource',
                                    titleCode     : 'project.configuration.extra.category.resourceModelSource.title',
                                    helpCode      : 'project.configuration.extra.category.resourceModelSource.description'
                          ]}"/>

                <g:if test="${resourceModelConfigDescriptions}">
                  <div class="list-group-item">
                    <span class="h4">
                      <g:message code="project.node.sources.title"/>
                    </span>
                    <div class="help-block">
                      <g:message code="domain.Project.edit.ResourceModelSource.explanation"/>
                    </div>
                    <ol id="configs">
                      <g:if test="${configs}">
                        <g:each var="config" in="${configs}" status="n">
                          <li>
                            <div class="inpageconfig">
                              <div class="panel panel-default">
                                <div class="panel-body">
                                  <g:set var="desc" value="${resourceModelConfigDescriptions.find {it.name == config.type}}"/>
                                    <g:if test="${!desc}">
                                      <span class="warn note invalidProvider">Invalid Resource Model Source configuration: Provider not found: <g:enc>${config.type}</g:enc></span>
                                    </g:if>
                                    <g:render template="viewResourceModelConfig"
                                              model="${[prefix   : prefixKey + '.' + (n + 1) + '.', values: config.props, includeFormFields: false, description: desc, saved: true, type: config.type]}"/>
                                      <g:set var="writeableSource" value="${writeableSources.find { it.index == (n + 1) }}"/>
                                      <g:if test="${writeableSource}">
                                        <div class="row row-space-top">
                                          <div class="col-sm-12">
                                            <g:link
                                                    class="btn btn-sm btn-default"
                                                    action="editProjectNodeSourceFile"
                                                    controller="framework"
                                                    params="${[project: project, index: (n + 1)]}">
                                              <g:icon name="pencil"/>
                                              <g:message code="edit.nodes.file"/>
                                            </g:link>
                                          </div>
                                        </div>
                                      </g:if>
                                      <g:if test="${parseExceptions[(n+1)+'.source']}">
                                        <div class="row row-space">
                                          <div class="col-sm-12">
                                            <span class="text-danger">${parseExceptions[(n+1)+'.source']?.message}</span>
                                          </div>
                                        </div>
                                      </g:if>
                                    </div>
                                  </div>
                                </div>
                              </li>
                            </g:each>
                          </g:if>
                        </ol>
                      </g:if>
                    </div>
                  </div>
                </div>
          </div>
        </div>
      </g:form>
    </div>
  </div>
</body>
</html>
