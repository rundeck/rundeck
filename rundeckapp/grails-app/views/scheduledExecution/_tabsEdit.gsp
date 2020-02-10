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



<div class="vue-tabs">
    <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
            <ul class="nav nav-tabs" id="job_edit_tabs" data-ko-bind="jobeditor">
                <li class="active">
                    <a href="#tab_details" data-toggle="tab">
                        <g:message code="job.edit.page.tab.details.title"/>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:'details',jobComponents:jobComponents)}"/>

                        <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                            <b class="text-warning fas fa-exclamation-circle"></b>
                        </g:if>
                    </a>
                </li>
                <li>
                    <a href="#tab_workflow" data-toggle="tab">
                        <g:message code="job.edit.page.tab.workflow.title"/>
                        <!-- ko if: inPageError() -->
                        <b class="text-warning fas fa-exclamation-circle"></b>
                        <!-- /ko -->
                    </a>
                </li>
                <li>
                    <a href="#tab_nodes" data-toggle="tab">
                        <g:message code="job.edit.page.tab.nodes.title"/>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:'nodes',jobComponents:jobComponents)}"/>

                        <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                            <b class="text-warning fas fa-exclamation-circle"></b>
                        </g:if>
                    </a>
                </li>
                <li>
                    <a href="#tab_schedule" data-toggle="tab">
                        <g:message code="job.edit.page.tab.schedule.title"/>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:'schedule',jobComponents:jobComponents)}"/>

                        <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                            <b class="text-warning fas fa-exclamation-circle"></b>
                        </g:if>
                    </a>
                </li>
                <li>
                    <a href="#tab_notifications" data-toggle="tab">
                        <g:message code="job.edit.page.tab.notifications.title"/>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:'notifications',jobComponents:jobComponents)}"/>

                        <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                            <b class="text-warning fas fa-exclamation-circle"></b>
                        </g:if>
                    </a>
                </li>
                <feature:enabled name="executionLifecyclePlugin">
                    <g:if test="${executionLifecyclePlugins}">
                        <li>
                            <a href="#tab_execution_plugins" data-toggle="tab">
                                <g:message code="job.edit.page.tab.execution.plugins.title" default="Execution Plugins"/>
                                <!-- ko if: pluginsError() -->
                                <b class="text-warning fas fa-exclamation-circle"></b>
                                <!-- /ko -->
                            </a>
                        </li>
                    </g:if>
                </feature:enabled>
                <li>
                    <a href="#tab_other" data-toggle="tab">
                        <g:message code="job.edit.page.tab.other.title"/>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:'other',jobComponents:jobComponents)}"/>

                        <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                            <b class="text-warning fas fa-exclamation-circle"></b>
                        </g:if>
                    </a>
                </li>

                <g:set var="componentSections" value="${g.
                        jobComponentSections(
                                jobComponents: jobComponents,
                                defaultSection: 'other',
                                skipSections: ['details', 'workflow', 'nodes', 'schedule', 'notifications', 'other']
                        )}"/>

                <g:each var="sectionName" in="${componentSections.keySet()}">
                    <li>
                        <g:set var="sectionProps" value="${g.jobComponentSectionProperties(section:sectionName,jobComponents:jobComponents)}"/>
                        <a href="#tab_${enc(attr:sectionName)}" data-toggle="tab">
                            ${componentSections[sectionName].title?:sectionName}

                            <g:if test="${sectionProps.any{jobComponentValidation?.get(it.name)}}">
                                <b class="text-warning fas fa-exclamation-circle"></b>
                            </g:if>
                        </a>
                    </li>
                </g:each>
            </ul>
        </div>
    </div>
</div>

<div class="tab-content" id="page_job_edit">

    <g:render template="edit"
              model="['scheduledExecution': scheduledExecution, 'crontab': crontab, authorized: authorized, sessionOpts: sessionOpts, jobComponents: jobComponents]"/>
</div>
