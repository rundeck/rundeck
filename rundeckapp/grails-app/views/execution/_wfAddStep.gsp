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
<div class="panel-heading">
    <span class="h3 ">
        <g:message code="${addMessage}"/>
    </span>
</div>
<div class=" add_step_buttons panel-body">
<div class="row">
    <div class="col-sm-12">
        <g:if test="${descriptionMessage}">
            <span><g:message code="${descriptionMessage}"/></span>
        </g:if>
        <div class="h4"><g:message code="${chooseMessage}"/></div>
    </div>
</div>
<div class="row row-space">
<div class="col-sm-12">
    <ul class="nav nav-tabs" >
        <li class="active node_step_section">
            <a href="#addnodestep" data-toggle="tab">
                <g:message code="node.steps" />
            </a>
        </li>
        <li class="step_section">
            <a href="#addwfstep" data-toggle="tab">
                <g:message code="framework.service.WorkflowStep.label.plural" />
            </a>
        </li>
    </ul>
    <div class="tab-content">
            <div class="node_step_section tab-pane active " id="addnodestep">
                <div class="list-group">
                <div class="list-group-item">
                    <span class=" list-group-item-heading h4 text-info">
                        <g:message code="framework.service.WorkflowNodeStep.description" />
                    </span>
                </div>
                    <a class="list-group-item  add_node_step_type" data-node-step-type="command" href="#">
                        <i class="rdicon icon-small shell"></i>
                        <g:message code="step.type.exec.title"/>
                        <span class="text-info">- <g:message code="step.type.exec.description"/></span>
                    </a>
                    <a class="list-group-item textbtn  add_node_step_type" href="#" data-node-step-type="script">
                        <i class="rdicon icon-small script"></i>
                        <g:message code="step.type.script.title"/>
                        <span class="text-info">- <g:message code="step.type.script.description"/></span>
                    </a>
                    <a class="list-group-item textbtn  add_node_step_type" href="#" data-node-step-type="scriptfile">
                        <i class="rdicon icon-small scriptfile"></i>
                        <g:message code="step.type.scriptfile.title"/>
                        <span class="text-info">- <g:message code="step.type.scriptfile.description"/></span>
                    </a>
                    <a class="list-group-item textbtn add_node_step_type" data-node-step-type="job" href="#">
                        <i class="glyphicon glyphicon-book"></i>
                        <g:message code="step.type.jobreference.title"/>
                        <span class="text-info">- <g:message code="step.type.jobreference.nodestep.description"/></span>
                    </a>
                <g:if test="${nodeStepDescriptions}">
                    <div class="list-group-item text-muted ">
                        <g:plural for="${nodeStepDescriptions}" code="node.step.plugin" />
                    </div>
                    <g:each in="${nodeStepDescriptions.sort{a,b->a.name<=>b.name}}" var="typedesc">

                        <a  class="list-group-item textbtn  add_node_step_type" data-node-step-type="${enc(attr:typedesc.name)}"
                            href="#">
                            <stepplugin:pluginIcon service="WorkflowNodeStep"
                                                   name="${typedesc.name}"
                                                   width="16px"
                                                   height="16px">
                                <i class="rdicon icon-small plugin"></i>
                            </stepplugin:pluginIcon>
                            <stepplugin:message
                                    service="WorkflowNodeStep"
                                    name="${typedesc.name}"
                                    code="plugin.title"
                                    default="${typedesc.title}"/>
                            <span class="text-info">-
                            <g:render template="/scheduledExecution/description"
                                      model="[description: stepplugin.messageText(
                                              service: 'WorkflowNodeStep',
                                              name: typedesc.name,
                                              code: 'plugin.description',
                                              default: typedesc.description
                                      ), textCss         : '',
                                              mode       : 'hidden', rkey: g.rkey()]"/>
                            </span>
                        </a>
                    </g:each>
                </g:if>
                </div>
            </div>
            <div class="step_section tab-pane " id="addwfstep">
                <div class="list-group">
                <div class="list-group-item">
                    <span class=" list-group-item-heading h4 text-info">
                        <g:message code="framework.service.WorkflowStep.description" />
                    </span>
                </div>

                <a class="list-group-item textbtn add_step_type" data-step-type="job" href="#">
                    <i class="glyphicon glyphicon-book"></i>
                    <g:message code="step.type.jobreference.title" /> <span class="text-info">- <g:message code="step.type.jobreference.description" /></span>
                </a>
                <g:if test="${stepDescriptions}">
                    <div class="list-group-item text-muted ">
                        <g:plural for="${stepDescriptions}" code="workflow.step.plugin" />
                    </div>
                    <g:each in="${stepDescriptions.sort{a,b->a.name<=>b.name}}" var="typedesc">
                        <a class="list-group-item textbtn  add_step_type" data-step-type="${enc(attr:typedesc.name)}" href="#">
                            <stepplugin:pluginIcon service="WorkflowStep"
                                                   name="${typedesc.name}"
                                                   width="16px"
                                                   height="16px">
                                <i class="rdicon icon-small plugin"></i>
                            </stepplugin:pluginIcon>
                            <stepplugin:message
                                    service="WorkflowStep"
                                    name="${typedesc.name}"
                                    code="plugin.title"
                                    default="${typedesc.title}"/>
                            <span class="text-info">-
                                <g:render template="/scheduledExecution/description"
                                          model="[description:
                                                          stepplugin.messageText(
                                                                  service: 'WorkflowStep',
                                                                  name: typedesc.name,
                                                                  code: 'plugin.description',
                                                                  default: typedesc.description
                                                          ),
                                                  textCss    : '',
                                                  mode       : 'hidden', rkey: g.rkey()]"/>
                            </span>
                        </a>
                    </g:each>

                </g:if>
                </div>
            </div>
    </div>
</div>
</div>
</div>

<div class="panel-footer">
    <span class="btn btn-default btn-sm cancel_add_step_type" ><g:message code="button.action.Cancel" /></span>
</div>
