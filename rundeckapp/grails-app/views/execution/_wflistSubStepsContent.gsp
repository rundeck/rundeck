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
<%@ page import="org.rundeck.app.data.workflow.ConditionalStep" %>
<div class="wfsubstepsgroup">
    <span class="wfsubstepsheading text-strong"><g:message code="Workflow.conditional.subSteps.label"/></span>
    <ul class="wfsubstepsitem">
        <g:each in="${steps}" var="subItem" status="j">
            <g:set var="subStepLabel" value="${labelPrefix ? labelPrefix + '.' + (j + 1) : (j + 1)}"/>
            <li class="wfsubstep-entry" id="wfli_${enc(attr:idPrefix)}${j}">
                <span class="wfsubstep-label text-muted"><g:enc>${subStepLabel}</g:enc>.</span>
                <g:render template="/execution/wflistitemContent"
                          model="${[i: idPrefix + j,
                                      stepNum: null,
                                      item: subItem,
                                      workflow: workflow,
                                      edit: edit,
                                      highlight: null,
                                      noimgs: noimgs,
                                      project: project]}"/>

                <g:if test="${subItem instanceof ConditionalStep && subItem.subSteps}">
                    <g:render template="/execution/wflistSubStepsContent"
                              model="${[steps: subItem.subSteps,
                                          workflow: workflow,
                                          edit: edit,
                                          noimgs: noimgs,
                                          project: project,
                                          idPrefix: idPrefix + j + '_',
                                          labelPrefix: subStepLabel]}"/>
                </g:if>

                <g:if test="${subItem.errorHandler}">
                    <ul class="wfhandleritem">
                        <li id="wfli_eh_${enc(attr:idPrefix)}${j}">
                            <g:render template="/execution/wflistitemContent"
                                      model="${[i: 'eh_' + idPrefix + j,
                                                  stepNum: null,
                                                  item: subItem.errorHandler,
                                                  workflow: workflow,
                                                  edit: edit,
                                                  highlight: null,
                                                  noimgs: noimgs,
                                                  project: project,
                                                  isErrorHandler: true]}"/>
                        </li>
                    </ul>
                </g:if>
            </li>
        </g:each>
    </ul>
</div>
