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

<%@ page import="rundeck.UtilityTagLib" %>
<script type="text/javascript">

</script>

<g:set var="hasScheduleDefinitions" value="${scheduledExecution?.scheduleDefinitions?.size() > 0 ? true:false}"/>

<div class="tab-content">
    <div class="tab-pane active" id="projectSchedules">
        <div class="panel panel-default panel-tab-content crontab tabtarget">
            <div class="panel-body">
                <div class="container">
                    <div class="row">
                        <%
                            def scheduleDefinitions = scheduledExecution?scheduledExecution.scheduleDefinitions:null
                        %>
                        <g:render template="/scheduledExecution/detailsScheduleDefinitions" model="${[scheduleDefinitions:scheduleDefinitions,edit:false]}"/>
                    </div>
                    <div id="schednewbtn" style="margin:10px 0; ">
                        <span class="btn btn-default btn-sm ready" title="Associate Scheduled Definition" id="scheduleAssociate">
                            <b class="glyphicon glyphicon-plus"></b>
                            Associate Schedule Definition
                        </span>
                    </div>
                </div>
            </div>
        </div>
    </div>

</div>