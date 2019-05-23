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
            <ul class="nav nav-tabs" id="job_edit_tabs">
                <li class="active">
                    <a href="#tab_details" data-toggle="tab">
                        <g:message code="job.edit.page.tab.details.title"/>
                    </a>
                </li>
                <li>
                    <a href="#tab_workflow" data-toggle="tab">
                        <g:message code="job.edit.page.tab.workflow.title"/>
                    </a>
                </li>
                <li>
                    <a href="#tab_nodes" data-toggle="tab">
                        <g:message code="job.edit.page.tab.nodes.title"/>
                    </a>
                </li>
                <li>
                    <a href="#tab_schedule" data-toggle="tab">
                        <g:message code="job.edit.page.tab.schedule.title"/>
                    </a>
                </li>
                <li>
                    <a href="#tab_notifications" data-toggle="tab">
                        <g:message code="job.edit.page.tab.notifications.title"/>
                    </a>
                </li>
                <li>
                    <a href="#tab_other" data-toggle="tab">
                        <g:message code="job.edit.page.tab.other.title"/>
                    </a>
                </li>
            </ul>
        </div>
    </div>
</div>

<div class="tab-content" id="page_job_edit">

    <g:render template="edit"
              model="['scheduledExecution': scheduledExecution, 'crontab': crontab, authorized: authorized]"/>
</div>
