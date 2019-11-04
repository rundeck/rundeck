%{--
  - Copyright 2019 SimplifyOps, Inc. (http://simplifyops.com)
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
    _detailsScheduleDefinitions.gsp

    Author: Rodrigo Navarro <a href="mailto:rodrigo@rundeck.com">rodrigo@rundeck.com</a>
    Created: Oct 5, 2019 5:07:19 PM
 --%>
<g:each in="${scheduleDefinitions}" var="scheduleDef" status="i">
    <div class="col-sm-6">
        <g:set var="ukey" value="${g.rkey()}"/>
        <ul class="options">
            <li id="schedli_${i}" class="el-collapse-item scheduleEntry" style="" data-sched-index="${i}" data-sched-name="${scheduleDef?.name}">
                <span class="opt item " id="sched_${enc(attr:scheduleDef.name)}" >
                    <span><g:enc>${scheduleDef.name} - ${scheduleDef.generateCrontabExression()}</g:enc></span>
                    <span class="btn btn-xs btn-danger pull-right"
                          data-toggle="collapse"
                          data-target="#scheddel_${enc(attr:ukey)}"
                          title="${message(code:"delete.this.schedule")}">
                        <i class="glyphicon glyphicon-remove"></i>
                    </span>
                </span>
                <div id="scheddel_${enc(attr:ukey)}" class="panel panel-danger collapse">
                    <div class="panel-heading">
                        <g:message code="delete.this.option" />
                    </div>

                    <div class="panel-body">
                        <g:message code="really.delete.option.0" args="${[scheduleDef.name]}"/>
                    </div>

                    <g:jsonToken id="reqtoken_del_${ukey}" url="${request.forwardURI}"/>
                    <div class="panel-footer">
                        <span class="btn btn-default btn-xs"
                              onclick="jQuery('#scheddel_${enc(js:ukey)}').collapse('toggle');"><g:message code="cancel"/></span>
                        <span class="btn btn-danger btn-xs"
                              onclick=" _doRemoveScheduleDefinition('${enc(js:scheduleDef.name)}', $(this).up('li.scheduleEntry'),'reqtoken_del_${enc(js:ukey)}');"><g:message
                                code="delete"/></span>
                    </div>
                </div>
            </li>
        </ul>
    </div>
</g:each>