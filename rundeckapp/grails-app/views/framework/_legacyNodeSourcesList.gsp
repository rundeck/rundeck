<div class="">
    <span class="h4">
        %{--
  - Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
                                          model="${[prefix: prefixKey + '.' +
                                                            (n + 1) +
                                                            '.', values: config.props, includeFormFields: false, description: desc, saved: true, type: config.type]}"/>
                                <g:set var="writeableSource"
                                       value="${writeableSources.find { it.index == (n + 1) }}"/>
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
                                <g:if test="${parseExceptions[(n + 1) + '.source']}">
                                    <div class="row row-space">
                                        <div class="col-sm-12">
                                            <span class="text-danger">${parseExceptions[(n + 1) + '.source']?.message}</span>
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
</div>
