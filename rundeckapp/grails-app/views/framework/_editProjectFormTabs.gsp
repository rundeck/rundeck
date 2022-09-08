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
<div class="vue-tabs"  data-ko-bind="editProject">
    <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
            <ul class="nav nav-tabs" id="job_edit_tabs">
                <li class="active">
                    <a href="#tab_details" data-toggle="tab">
                        <g:message code="project.edit.page.tab.details.title"/>
                    </a>
                </li>

                <feature:enabled name="cleanExecutionsHistoryJob">
                    <li>
                        <a href="#tab_history" data-toggle="tab">
                            <g:message code="execution.history.cleanup.label" default="Execution History Clean"/>
                        </a>
                    </li>
                </feature:enabled>
                <g:set var="categories" value="${new HashSet(
                        extraConfig?.values()?.
                                collect { it.configurable.categories?.values() }.
                                flatten()
                )}"/>
                <g:each in="${categories.sort() - 'resourceModelSource'}" var="category">
                    <li>
                        <a href="#tab_category_${category}" data-toggle="tab">
                            <g:message code="project.configuration.extra.category.${category}.title"
                                       default="${category}"/>
                        </a>
                    </li>
                </g:each>
                <g:each in="${serviceDefaultsList}" var="serviceDefaults">
                    <li>
                        <a href="#tab_svc_${serviceDefaults.service}" data-toggle="tab">
                            Default <g:message code="framework.service.${serviceDefaults.service}.label"/>
                        </a>
                    </li>
                </g:each>

                <g:if test="${pluginGroupDefined}">
                    <li>
                        <a href="#tab_pluginGroups" data-toggle="tab">
                            <g:message code="project.edit.page.tab.plugins.title" default="Plugins"/>
                        </a>
                    </li>
                </g:if>
            </ul>
        </div>
    </div>
</div>


<div class="tab-content spacing-lg" id="page_job_edit" data-ko-bind="editProject">
    <g:render template="editProjectForm" model="${[editOnly: editOnly, project: project,serviceDefaultsList:serviceDefaultsList]}"/>
</div>
