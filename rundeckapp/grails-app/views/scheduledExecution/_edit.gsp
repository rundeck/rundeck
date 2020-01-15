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

<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants; rundeck.ScheduledExecution; rundeck.User; org.rundeck.core.auth.AuthConstants" %>

<g:jsonToken id="job_edit_tokens" url="${request.forwardURI}"/>

<g:if test="${flash.message}">
    <div class="list-group-item">
      <div class="alert alert-info">
        <g:enc>${flash.message}</g:enc>
      </div>
    </div>
</g:if>
<g:hasErrors bean="${scheduledExecution}">
    <div class="list-group-item">
      <div class="alert alert-danger">
        <g:renderErrors bean="${scheduledExecution}" as="list"/>
      </div>
    </div>
</g:hasErrors>
<g:set var="labelColSize" value="col-sm-2"/>
<g:set var="labelColClass" value="${labelColSize}  control-label"/>
<g:set var="fieldColSize" value="col-sm-10"/>
<g:set var="fieldColHalfSize" value="col-sm-5"/>
<g:set var="fieldColShortSize" value="col-sm-4"/>
<g:set var="offsetColSize" value="col-sm-10 col-sm-offset-2"/>

<g:set var="editSchedExecId" value="${scheduledExecution?.id? scheduledExecution.extid:null}"/>

<asset:javascript src="prototype/effects"/>
<asset:javascript src="prototype/dragdrop"/>
<g:if test="${schedulesEnabled}">
    <asset:javascript src="rundeckpro-schedules/static/js/vendor.js" defer="defer"/>
    <asset:javascript src="rundeckpro-schedules/static/js/schedules.js" defer="defer"/>
    <asset:stylesheet href="rundeckpro-schedules/static/css/schedules.css"/>
</g:if>
<g:set var="project" value="${scheduledExecution?.project ?: params.project?:request.project?: projects?.size() == 1 ? projects[0].name : ''}"/>
<g:embedJSON id="filterParamsJSON"
             data="${[filterName: params.filterName, filter: scheduledExecution?.asFilter(),filterExcludeName: params.filterExcludeName, filterExclude: scheduledExecution?.asExcludeFilter(),nodeExcludePrecedence: scheduledExecution?.nodeExcludePrecedence, excludeFilterUncheck: scheduledExecution?.excludeFilterUncheck]}"/>
<g:embedJSON id="jobDefinitionJSON"
             data="${[jobName:scheduledExecution?.jobName,groupPath:scheduledExecution?.groupPath, uuid: scheduledExecution?.uuid,
                     href:scheduledExecution?.id?createLink(controller:'scheduledExecution',action:'show',params:[project:scheduledExecution.project,id:scheduledExecution.extid]):null
             ]}"/>

  <g:if test="${scheduledExecution && scheduledExecution.id}">
      <input type="hidden" name="id" value="${enc(attr:scheduledExecution.extid)}"/>
  </g:if>

  <div class="alert alert-danger" style="display: none" id="editerror">

  </div>

  <div class="tab-pane active" id="tab_details" data-ko-bind="jobeditor">
  <section class="section-space-lg">
          %{--name--}%
      <div class="form-group ${g.hasErrors(bean:scheduledExecution,field:'jobName','has-error')}" id="schedJobNameLabel">
          <label for="schedJobName"
                 class="required ${enc(attr:labelColClass)}"
                 >
              <g:message code="scheduledExecution.jobName.label" />
          </label>
          <div class="${fieldColHalfSize}">
              <g:textField name="jobName"
                           value="${scheduledExecution?.jobName}"
                           id="schedJobName"
                           class="form-control"
                  data-bind="value: jobName"
              />
              <g:hasErrors bean="${scheduledExecution}" field="jobName">
                  <i alt="Error" id="schedJobNameErr" class="glyphicon glyphicon-warning-sign"></i>
                  <wdgt:eventHandler for="schedJobName" state="unempty"  frequency="1">
                      <wdgt:action target="schedJobNameLabel" removeClassname="has-error"/>
                      <wdgt:action visible="false" target="schedJobNameErr"/>
                  </wdgt:eventHandler>
              </g:hasErrors>
          </div>
          %{--group--}%

          <div class="${fieldColHalfSize}">
              <div class="input-group">
                  <g:hasErrors bean="${scheduledExecution}" field="groupPath">
                      <span class="input-group-addon">
                        <i class="glyphicon glyphicon-warning-sign"></i>
                      </span>
                  </g:hasErrors>
                  <input type='text' name="groupPath" value="${enc(attr:scheduledExecution?.groupPath)}"
                      data-bind="value: groupPath"
                         id="schedJobGroup"
                      class="form-control"
                      placeholder="${g.message(code:'scheduledExecution.groupPath.description')}"
                  />

                  <span class="input-group-btn">
                      <span class="btn btn-default"
                            data-toggle="modal"
                            data-target="#groupChooseModal"
                            title="${message(code:"job.edit.groupPath.choose.text")}">
                          <g:message code="choose.action.label" />
                      </span>
                  </span>
              </div>


          </div>
          <g:render template="/common/modal" model="[modalid:'groupChooseModal',titleCode:'job.edit.groupPath.choose.text']">
                <div id="groupChooseModalContent">

                </div>
          </g:render>
      </div>

          %{--description--}%
      <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'description', 'has-error')}">
          <label for="description" class="${labelColClass}"><g:message code="scheduledExecution.property.description.label" /></label>
          <div class="${fieldColSize}">
              <ul class="nav nav-tabs">

                  <li class="active"><a href="#desceditor" data-toggle="tab">Edit</a></li>
                  <li id="previewrunbook" style="${wdgt.styleVisible(if: g.textHasMarker(text:scheduledExecution?.description,marker:ScheduledExecution.RUNBOOK_MARKER))}">
                      <a href="#descpreview" data-toggle="tab">
                          <g:message code="job.editor.preview.runbook" />
                      </a>
                  </li>
              </ul>

              <div class="tab-content">

                  <div class="tab-pane active" id="desceditor">
                      <g:textArea name="description"
                                  value="${scheduledExecution?.description}"
                                  cols="80"
                                  rows="3"
                                  class="form-control ace_editor _job_description"
                                  data-ace-session-mode="markdown"
                                  data-ace-height="120px"
                                  data-ace-resize-auto="true"
                                  data-ace-resize-max="30"
                      />
                  </div>
                  <div class="tab-pane panel panel-default panel-tab-content" id="descpreview">
                      <div class="panel-body" id="descpreview_content"></div>
                  </div>
              </div>
              <g:hasErrors bean="${scheduledExecution}" field="description">
                  <i class="glyphicon glyphicon-warning-sign text-warning"></i>
              </g:hasErrors>
              <g:set var="allowHTML"
                     value="${!(grailsApplication.config.rundeck?.gui?.job?.description?.disableHTML in [true, 'true'])}"/>
              <div class="help-block">
                  <g:if test="${allowHTML}">
                      <g:render template="/scheduledExecution/description"
                                model="${[description: g.message(code:"ScheduledExecution.property.description.description"),mode:'collapsed',rkey:g.rkey()]}"/>
                  </g:if>
                  <g:else>
                      <g:message code="ScheduledExecution.property.description.plain.description"/>
                  </g:else>
              </div>
              <g:javascript>
                  jQuery(function () {
                      jQuery('textarea.ace_editor._job_description').each(function () {
                          var editor = _addAceTextarea(this, function (editor) {
                              "use strict";
                              //test if a runbook was added, enable preview tab
                              if (_hasJobDescriptionRunbook(editor.getValue())) {
                                  jQuery('#previewrunbook').show();
                              }else{
                                  jQuery('#previewrunbook').hide();
                              }
                          });

                          _setupMarkdeepPreviewTab('previewrunbook', 'descpreview_content', function () {
                              return _jobDescriptionRunbook(editor.getValue());
                          });

                      });
                  });
              </g:javascript>
          </div>
      </div>
  </section><!--/.nput-group-item -->
  </div><!-- end #tab_details -->

      <g:set var="projectName" value="${scheduledExecution.project?scheduledExecution.project.toString():params.project ?: request.project?: projects?.size() == 1 ? projects[0].name : ''}" />
      <g:hiddenField id="schedEditFrameworkProject" name="project" value="${projectName}" />

      %{--Options--}%
    <div class="tab-pane" id="tab_workflow">
      <section id="optionsContent" class=" section-space-lg" >
          <div class="form-group">
              <div class="${labelColSize} control-label text-form-label"><span id="optsload"></span><g:message code="options.label" /></div>
              <div class="${fieldColSize}">

                  <div  id="editoptssect" class="rounded">
                      <%
                          def options = sessionOpts
                          if(!options){
                              def tmpse = ScheduledExecution.get(scheduledExecution.id)
                              options = tmpse?tmpse.options:scheduledExecution.options
                          }
                      %>
                      <g:render template="/scheduledExecution/detailsOptions" model="${[options:options,edit:true]}"/>
                      <g:if test="${scheduledExecution && scheduledExecution.argString}">
                          <g:render template="/execution/execArgString" model="[argString: scheduledExecution.argString]"/>
                      </g:if>
                      <g:hiddenField name="_sessionopts" value="true"/>

                  </div>
              </div>
          </div>
      </section>%{--//Options--}%

      %{--Workflow--}%
      <section id="workflowContent" class="section-separator section-space-lg" >
          <div class="form-group">
              <div class="${labelColSize}  control-label text-form-label"><g:message code="Workflow.label" /></div>
              <div class="${fieldColSize}" style="padding-top:1em;">
                  <g:set var="editwf" value="${session.editWF && session.editWF[scheduledExecution.id.toString()]?session.editWF[scheduledExecution.id.toString()]:scheduledExecution.workflow}"/>
                  <g:render template="/execution/execDetailsWorkflow" model="${[workflow:editwf,context:scheduledExecution,edit:true,error:scheduledExecution?.errors?.hasFieldErrors('workflow'),project:scheduledExecution?.project?:(params.project ?: request.project)?: projects?.size() == 1 ? projects[0].name :'',
                                                                                strategyPlugins:strategyPlugins]}"/>
                  <g:hiddenField name="_sessionwf" value="true"/>
                  <g:if test="${null==editwf || null==editwf.commands || 0==editwf.commands.size()}">
                      <g:javascript>
                          fireWhenReady('workflowContent',function(){
                              $('wfnewtypes').show();
                              $('wfnewbutton').hide();
                          });
                      </g:javascript>
                  </g:if>
              </div>
          </div>
      </section>%{--//Workflow--}%
</div><!-- end#tab_workflow -->

  %{--Node Dispatch--}%
    <div class="tab-pane" id="tab_nodes">
  <section class="section-space-lg node_filter_link_holder" id="nodegroupitem">
  <div class="form-group">
      <label class="${labelColSize} control-label">
          <g:message code="Node.plural" />
      </label>

      <div class="${fieldColSize} ">
        <div class="radio radio-inline">
          <input type="radio"
                 name="doNodedispatch"
                 value="true"
                  class="node_dispatch_radio"
              ${scheduledExecution?.doNodedispatch ? 'checked' : ''}
                 id="doNodedispatchTrue"/>
          <label for="doNodedispatchTrue">
               <g:message code="dispatch.to.nodes" />
          </label>
        </div>
        <div class="radio radio-inline">
          <input id="doNodedispatchFalse"
                 type="radio"
                 name="doNodedispatch"
                 value="false"
                 class="node_dispatch_radio"
              ${!scheduledExecution?.doNodedispatch ? 'checked' : ''}/>
          <label for="doNodedispatchFalse">
              <g:message code="execute.locally" />
          </label>
        </div>
      </div>
  </div>

  <div class="form-group">
      <div class="${offsetColSize}">
          <span class="help-block">
              <g:message code="scheduledExecution.property.doNodedispatch.description" />
          </span>

          <g:javascript>
              <wdgt:eventHandlerJS for="doNodedispatchTrue" state="unempty" oneway="true">
                  <wdgt:action visible="true" targetSelector=".nodeFilterFields"/>
                  <wdgt:action visible="true" targetSelector=".nodeFilterExcludeFields"/>
                  <wdgt:action visible="true" target="nodeDispatchFields"/>
              </wdgt:eventHandlerJS>
              <wdgt:eventHandlerJS for="doNodedispatchFalse" state="unempty" oneway="true">
                  <wdgt:action visible="false" target="nodeDispatchFields"/>
                  <wdgt:action visible="false" targetSelector=".nodeFilterFields"/>
                  <wdgt:action visible="false" targetSelector=".nodeFilterExcludeFields"/>
              </wdgt:eventHandlerJS>
          </g:javascript>
      </div>
  </div>

  <div class="form-group  ${hasErrors(bean: scheduledExecution, field: 'filter', 'has-error')}">
  <div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields ">
      <label class="${labelColSize} control-label">
          <g:message code="node.filter" />
      </label>

      <div class="${fieldColSize}">
          <g:hiddenField name="formInput" value="true"/>
          <g:hasErrors bean="${scheduledExecution}" field="filter">

              <div class="text-warning">
                  <g:renderErrors bean="${scheduledExecution}" as="list" field="filter"/>
                  <i class="glyphicon glyphicon-warning-sign"></i>
              </div>

          </g:hasErrors>
          <g:set var="filtvalue" value="${scheduledExecution.asFilter()}"/>

                  <span class="input-group nodefilters multiple-control-input-group">
                      <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                          <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
                      </g:if>
                      <g:render template="/framework/nodeFilterInputGroup"
                                model="[filterset: filterset, filtvalue: filtvalue, filterName: filterName]"/>
                  </span>

          <div class=" collapse" id="queryFilterHelp">
              <div class="help-block">
                  <g:render template="/common/nodefilterStringHelp"/>
              </div>
          </div>


      </div>


  </div>
  </div>

  <div class="form-group  ${hasErrors(bean: scheduledExecution, field: 'filterExclude', 'has-error')}">
    <div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields ">
        <label class="${labelColSize} control-label">
            <g:message code="node.filter.exclude" />
        </label>

        <div class="${fieldColSize}">
            <g:set var="excludeFilterValue" value="${scheduledExecution.asExcludeFilter()}"/>

            <span class="input-group  multiple-control-input-group">
                <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                    <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters}"/>
                </g:if>
                <g:render template="/framework/nodeFilterInputExcludeGroup"
                          model="[filterset: filterset, filtvalue: excludeFilterValue, filterName: filterName]"/>
            </span>

            <span class="help-block">
                <g:message code="scheduledExecution.property.excludeFilter.description" />
            </span>
        </div>


    </div>
  </div>
  <div class="form-group">
        <div class="${labelColSize} control-label text-form-label">
            <g:message code="scheduledExecution.property.excludeFilterUncheck.label"/>
        </div>

        <div class="${fieldColSize}">
            <div class="radio radio-inline">
                <g:radio name="excludeFilterUncheck" value="true"
                         checked="${scheduledExecution.excludeFilterUncheck}"
                         data-bind="checked: excludeFilterUncheck"
                         id="editableTrue"/>
                <label for="editableTrue">
                    <g:message code="yes"/>
                </label>
            </div>

            <div class="radio radio-inline">
                <g:radio value="false" name="excludeFilterUncheck"
                         checked="${!scheduledExecution.excludeFilterUncheck}"
                         data-bind="checked: excludeFilterUncheck"
                         id="editableFalse"/>
                <label for="editableFalse">
                    <g:message code="no"/>
                </label>
            </div>

            <span class="help-block">
                <g:message code="scheduledExecution.property.excludeFilterUncheck.description" />
            </span>
        </div>
    </div>


  <div style="${wdgt.styleVisible(if: scheduledExecution?.doNodedispatch)}" class="subfields nodeFilterFields ">
  <g:if test="${grailsApplication.config.rundeck?.nodefilters?.showPrecedenceOption || scheduledExecution?.nodeExcludePrecedence!=null && !scheduledExecution?.nodeExcludePrecedence }">

      <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeInclude', 'has-error')}">

          <label class="col-sm-2  control-label"><g:message code="precedence.to.prompt" /></label>

          <div class="col-sm-10">
              <label title="Include more nodes" class="radio-inline">
                  <g:radio name="nodeExcludePrecedence" value="false"
                           data-bind="checked: nodeExcludePrecedence"
                           checked="${!scheduledExecution?.nodeExcludePrecedence}"
                           id="nodeExcludePrecedenceFalse"/>
                  <g:message code="included" /></label>

              <label title="Exclude more nodes" class="radio-inline">
                  <g:radio name="nodeExcludePrecedence" value="true"
                           data-bind="checked: nodeExcludePrecedence"
                           checked="${scheduledExecution?.nodeExcludePrecedence}"
                           id="nodeExcludePrecedenceTrue"/>
                  <g:message code="excluded" /></label>
          </div>
      </div>%{--//extended filters--}%

  </g:if>
  <g:else>
      <g:hiddenField name="nodeExcludePrecedence" value="true"/>
  </g:else>

  <div class="subfields nodeFilterFields">

      <div class="form-group">
          <label class="${labelColClass}">
              <g:message code="matched.nodes.prompt" />
          </label>

          <div class=" col-sm-10  container-fluid">

              <div class="well well-sm matchednodes">
                  <div class="row">
                      <div class="col-sm-6">
                          <span class="text-info" data-bind="if: loaded() && !loading()">
                              <span data-bind="messageTemplate: [total,nodesTitle]"><g:message
                                      code="count.nodes.matched"/></span>
                          </span>
                          <span data-bind="visible: loading() " class="text-muted">
                              <i class="glyphicon glyphicon-time"></i>
                              <g:message code="loading.matched.nodes"/>
                          </span>

                          <span data-bind="if: total()>maxShown()">
                              <span data-bind="messageTemplate: [maxShown(), total()]" class="text-primary">
                                  <g:message code="count.nodes.shown"/>
                              </span>
                          </span>
                      </div>

                      <div class="col-sm-6">

                          <button type="button" class="pull-right btn btn-info btn-sm refresh_nodes"
                                  data-loading-text="${g.message(code: 'loading')}"
                                  data-bind="click: $data.updateMatchedNodes, attr: {disabled: loading}"
                                  title="${g.message(code: 'click.to.refresh')}">
                              <g:message code="refresh"/>
                              <i class="glyphicon glyphicon-refresh"></i>
                          </button>

                      </div>
                  </div>
                  <div id='matchednodes' class="clearfix row">
                      <div class="col-sm-12 container-fluid">
                          <g:render template="/framework/nodesEmbedKO"
                                    model="[showLoading: false, showTruncated: false, showExcludeFilterLinks: true]"/>
                      </div>
                  </div>
              </div>
          </div>
      </div>


      <div id="nodeDispatchFields" class="subfields ">

          <div class="form-group">
              <div class="${labelColSize} control-label text-form-label">
                  <g:message code="scheduledExecution.property.nodefiltereditable.label"/>
              </div>

              <div class="${fieldColSize}">
                <div class="radio radio-inline">
                  <g:radio value="false" name="nodeFilterEditable"
                           checked="${!scheduledExecution.nodeFilterEditable}"
                           id="editableFalse"/>
                  <label for="editableFalse">
                      <g:message code="no"/>
                  </label>
                </div>
                <div class="radio radio-inline">
                  <g:radio name="nodeFilterEditable" value="true"
                           checked="${scheduledExecution.nodeFilterEditable}"
                           id="editableTrue"/>
                  <label for="editableTrue">
                      <g:message code="yes"/>
                  </label>
                </div>
              </div>
          </div>

          <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeThreadcountDynamic', 'has-error')}">
              <label for="schedJobnodeThreadcount" class="${labelColClass}">
                  <g:message code="scheduledExecution.property.nodeThreadcount.label"/>
              </label>

              <div class="${fieldColSize}">
                  <div class="row">
                  <div class="col-sm-4">
                  <input type='text' name="nodeThreadcountDynamic"
                         value="${enc(attr:scheduledExecution?.nodeThreadcountDynamic)}" id="schedJobnodeThreadcount"
                         size="3"
                         class="form-control input-sm"/>
                  </div>
                  </div>
                  <g:hasErrors bean="${scheduledExecution}" field="nodeThreadcountDynamic">
                      <div class="text-warning">
                          <i class="glyphicon glyphicon-warning-sign"></i>
                          <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeThreadcountDynamic"/>
                      </div>
                  </g:hasErrors>
                  <span class="help-block">
                      <g:message code="scheduledExecution.property.nodeThreadcount.description"/>
                  </span>

              </div>
          </div>

          <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'nodeRankAttribute', 'has-error')}">
              <label for="schedJobnodeRankAttribute" class="${labelColClass}">
                  <g:message code="scheduledExecution.property.nodeRankAttribute.label"/>
              </label>

              <div class="${fieldColSize}">
                  <div class="row">
                      <div class="col-sm-4">
                  <input type='text' name="nodeRankAttribute"
                         value="${enc(attr:scheduledExecution?.nodeRankAttribute)}" id="schedJobnodeRankAttribute"
                         class="form-control input-sm"/>
                      </div>
                  </div>
                  <g:hasErrors bean="${scheduledExecution}" field="nodeRankAttribute">
                      <div class="text-warning">
                          <i class="glyphicon glyphicon-warning-sign"></i>
                          <g:renderErrors bean="${scheduledExecution}" as="list" field="nodeRankAttribute"/>
                      </div>
                  </g:hasErrors>
                  <span class="help-block">
                      <g:message code="scheduledExecution.property.nodeRankAttribute.description"/>
                  </span>
              </div>
          </div>

          <div class="form-group">
              <label class="${labelColClass}">
                  <g:message code="scheduledExecution.property.nodeRankOrder.label"/>
              </label>

              <div class="${fieldColSize}">
                <div class="radio radio-inline">
                  <g:radio name="nodeRankOrderAscending" value="true"
                           checked="${scheduledExecution?.nodeRankOrderAscending || null == scheduledExecution?.nodeRankOrderAscending}"
                           id="nodeRankOrderAscending"/>
                   <label for="nodeRankOrderAscending">
                     <g:message code="scheduledExecution.property.nodeRankOrder.ascending.label"/>
                   </label>
                </div>
                <div class="radio radio-inline">
                  <g:radio name="nodeRankOrderAscending" value="false"
                           checked="${!scheduledExecution?.nodeRankOrderAscending && null != scheduledExecution?.nodeRankOrderAscending}"
                           id="nodeRankOrderDescending"/>
                   <label for="nodeRankOrderDescending">
                       <g:message code="scheduledExecution.property.nodeRankOrder.descending.label"/>
                   </label>
                </div>
              </div>
          </div>

          <div class="form-group">
              <label class="${labelColClass}"><g:message code="scheduledExecution.property.nodeKeepgoing.prompt"/></label>

              <div class="${fieldColSize}">
                  <div class="radio">
                      <g:radio name="nodeKeepgoing" value="false" checked="${!scheduledExecution?.nodeKeepgoing}" id="nodeKeepgoingFalse"/>
                      <label for="nodeKeepgoingFalse">
                          <g:message code="scheduledExecution.property.nodeKeepgoing.false.description"/>
                      </label>
                  </div>

                  <div class="radio">
                      <g:radio name="nodeKeepgoing" value="true" checked="${scheduledExecution?.nodeKeepgoing}" id="nodeKeepgoingTrue"/>
                      <label for="nodeKeepgoingTrue">
                          <g:message code="scheduledExecution.property.nodeKeepgoing.true.description"/>
                      </label>
                  </div>
              </div>
          </div>
          <div class="form-group">
              <label class="${labelColClass}"><g:message code="scheduledExecution.property.successOnEmptyNodeFilter.prompt"/></label>

              <div class="${fieldColSize}">
                  <div class="radio">
                      <g:radio name="successOnEmptyNodeFilter"
                             value="false" checked="${!scheduledExecution?.successOnEmptyNodeFilter}" id="successOnEmptyNodeFilterFalse"/>
                      <label for="successOnEmptyNodeFilterFalse">
                          <g:message code="scheduledExecution.property.successOnEmptyNodeFilter.false.description"/>
                      </label>
                  </div>
                  <div class="radio">
                      <g:radio name="successOnEmptyNodeFilter" value="true" checked="${scheduledExecution?.successOnEmptyNodeFilter}" id="successOnEmptyNodeFilterTrue"/>
                      <label for="successOnEmptyNodeFilterTrue">
                          <g:message code="scheduledExecution.property.successOnEmptyNodeFilter.true.description"/>
                      </label>
                  </div>
              </div>
          </div>
          <div class="form-group">
              <label class="${labelColClass}"><g:message code="scheduledExecution.property.nodesSelectedByDefault.label"/></label>

              <div class="${fieldColSize}">
                  <div class="radio">
                    <g:radio
                            name="nodesSelectedByDefault"
                            value="true"
                            checked="${scheduledExecution.nodesSelectedByDefault==null||scheduledExecution.nodesSelectedByDefault}"
                            id="nodesSelectedByDefaultTrue"/>
                      <label for="nodesSelectedByDefaultTrue">
                          <g:message code="scheduledExecution.property.nodesSelectedByDefault.true.description"/>
                      </label>
                  </div>
                  <div class="radio">
                    <g:radio name="nodesSelectedByDefault"
                             value="false"
                             checked="${scheduledExecution.nodesSelectedByDefault!=null && !scheduledExecution.nodesSelectedByDefault}"
                             id="nodesSelectedByDefaultFalse"/>
                      <label for="nodesSelectedByDefaultFalse">
                          <g:message code="scheduledExecution.property.nodesSelectedByDefault.false.description"/>
                      </label>
                  </div>
              </div>
          </div>

          %{--orchestrator--}%
          <g:render template="editOrchestratorForm" model="[scheduledExecution:scheduledExecution, orchestratorPlugins: orchestratorPlugins,adminauth:adminauth]"/>
          %{--//orchestrator--}%
      </div>
  </div>

  </div>%{--//Node Dispatch--}%
  </section>
  </div><!-- end#tab_nodes-->

      %{--Notifications--}%
      <div class="tab-pane" id="tab_notifications"  >
      <section class="section-space-lg"  >
              <g:set var="adminauth"
                  value="${auth.resourceAllowedTest(type: 'project', name: scheduledExecution.project, action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_READ], context: 'application')}"/>

          <g:render template="editNotificationsForm" model="[scheduledExecution:scheduledExecution, notificationPlugins: notificationPlugins,adminauth:adminauth]"/>

      </section>%{--//Notifications--}%
      </div><!-- end#tab_notifications -->

  %{--Schedule--}%
    <div class="tab-pane" id="tab_schedule">
  <section class="section-space-lg">

      <div class="form-group">
          <div class="${labelColSize}  control-label text-form-label">
              <g:message code="schedule.to.run.repeatedly" />
          </div>
          <div class="${fieldColSize}">
              <div class="radio radio-inline">
                <g:radio name="scheduled" value="false"
                    checked="${scheduledExecution?.scheduled?false:true}"
                    id="scheduledFalse"/>
                <label for="scheduledFalse">
                  <g:message code="no" />
                </label>
              </div>
              <div class="radio radio-inline">
                <g:radio name="scheduled" value="true"
                    checked="${scheduledExecution?.scheduled}"
                    id="scheduledTrue"/>
                <label for="scheduledTrue">
                    <g:message code="yes" />
                </label>
              </div>
          </div>
          <div class="${offsetColSize}" style="${wdgt.styleVisible(if:scheduledExecution?.scheduled)}" id="scheduledExecutionEditCrontab">
              <g:render template="editCrontab" model="[scheduledExecution:scheduledExecution, crontab:crontab]"/>
          </div>
              <g:javascript>
                  <wdgt:eventHandlerJS for="scheduledTrue" state="unempty">
                      <wdgt:action visible="true" targetSelector="#scheduledExecutionEditCrontab"/>
                      <wdgt:action visible="true" targetSelector="#scheduledExecutionEditTZ"/>
                  </wdgt:eventHandlerJS>
                  <wdgt:eventHandlerJS for="scheduledFalse" state="unempty" >
                      <wdgt:action visible="false" target="scheduledExecutionEditCrontab"/>
                      <wdgt:action visible="false" targetSelector="#scheduledExecutionEditTZ"/>
                  </wdgt:eventHandlerJS>
              </g:javascript>
      </div>

      <div class="form-group" style="${wdgt.styleVisible(if:scheduledExecution?.scheduled)}" id="scheduledExecutionEditTZ">
          <div class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.timezone.prompt" />
          </div>
          <div class="${fieldColHalfSize}">
                  <input type='text' name="timeZone" value="${enc(attr:scheduledExecution?.timeZone)}"
                         id="timeZone" class="form-control"/>

                  <span class="help-block">
                      <g:message code="scheduledExecution.property.timezone.description" />
                  </span>
          </div>
      <g:javascript>
          fireWhenReady('timeZone',function(){
              var timeZonesDataArr = loadJsonData('timeZonesData');
              jQuery("#timeZone").devbridgeAutocomplete({
                  lookup: timeZonesDataArr
              });
          });
      </g:javascript>
      </div>

      <g:if test="${schedulesEnabled}">
      %{--schedule definitions--}%
          <div class="vue-job-schedules">
              <assigned-schedules-to-job
                      :event-bus="EventBus"
                      job-name="${scheduledExecution?.jobName}"
                      job-UUID="${scheduledExecution?.uuid}"
              ></assigned-schedules-to-job>
          </div>
      </g:if>

      %{-- scheduleEnabled --}%
      <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_TOGGLE_SCHEDULE)}">
          <div class="form-group">
              <div class="${labelColSize} control-label text-form-label">
                  <g:message code="scheduledExecution.property.scheduleEnabled.label"/>
              </div>

              <div class="${fieldColSize}">
                  <div class="radio radio-inline">
                      <g:radio name="scheduleEnabled"
                               value="true"
                               checked="${scheduledExecution.hasScheduleEnabled()}"
                               id="scheduleEnabledTrue"/>
                      <label for="scheduleEnabledTrue">
                        <g:message code="yes"/>
                      </label>
                  </div>

                  <div class="radio radio-inline">
                      <g:radio value="false"
                               name="scheduleEnabled"
                               checked="${!scheduledExecution.hasScheduleEnabled()}"
                               id="scheduleEnabledFalse"/>
                      <label for="scheduleEnabledFalse">
                        <g:message code="no"/>
                      </label>
                  </div>

                  <div class="help-block">
                      <g:message code="scheduledExecution.property.scheduleEnabled.description"/>
                  </div>
              </div>

          </div>
      </g:if>
      %{-- executionEnabled --}%
      <g:if test="${auth.jobAllowedTest(job: scheduledExecution, action: AuthConstants.ACTION_TOGGLE_EXECUTION)}">
          <div class="form-group">
              <div class="${labelColSize} control-label text-form-label">
                  <g:message code="scheduledExecution.property.executionEnabled.label"/>
              </div>

              <div class="${fieldColSize}">
                  <div class="radio radio-inline">
                      <g:radio name="executionEnabled" value="true"
                               checked="${scheduledExecution.hasExecutionEnabled()}"
                               id="executionEnabledTrue"/>
                      <label for="executionEnabledTrue"><g:message code="yes"/></label>
                  </div>

                  <div class="radio radio-inline">
                      <g:radio value="false" name="executionEnabled"
                               checked="${!scheduledExecution.hasExecutionEnabled()}"
                               id="executionEnabledFalse"/>
                      <label for="executionEnabledFalse">
                        <g:message code="no"/>
                      </label>
                  </div>

                  <div class="help-block">
                      <g:message code="scheduledExecution.property.executionEnabled.description"/>
                  </div>
              </div>
          </div>
      </g:if>
  </section>%{--//Schedule--}%
</div><!-- end#tab_schedule -->
<feature:enabled name="executionLifecyclePlugin">
    <g:if test="${executionLifecyclePlugins}">
        <g:set var="executionLifecyclePluginConfigMap" value="${scheduledExecution?.pluginConfigMap?.get('ExecutionLifecycle')?:[:]}"/>
        <div class="tab-pane" id="tab_execution_plugins">
            <div class="help-block">
                <g:message code="scheduledExecution.property.executionLifecyclePluginConfig.help.text" />
            </div>
            <div class="list-group">
                <g:each in="${executionLifecyclePlugins}" var="plugin">
                    <g:set var="pluginKey" value="${params.executionLifecyclePlugin?.type?.get(pluginType)?:g.rkey()}"/>
                    <g:set var="pluginType" value="${plugin.key}"/>
                    <g:hiddenField name="executionLifecyclePlugins.keys" value="${pluginKey}"/>
                    <g:hiddenField name="executionLifecyclePlugins.type.${pluginKey}" value="${pluginType}"/>
                    <g:set var="pluginDescription" value="${plugin.value.description}"/>
                    <g:set var="pluginConfig" value="${params.executionLifecyclePlugins?.get(pluginKey)?.configMap ?: executionLifecyclePluginConfigMap[pluginType]}"/>

                    <div class="list-group-item">
                        <g:if test="${pluginDescription}">
                            <div class="form-group">

                                <div class="col-sm-12">
                                    <div class="checkbox ">
                                        <g:set var="prkey" value="${rkey()}"/>
                                        <g:checkBox name="executionLifecyclePlugins.enabled.${pluginKey}" value="true"
                                                    class="form-control"
                                                    id="executionLifecyclePluginEnabled_${prkey}"
                                                    checked="${pluginConfig != null}"/>

                                        <label for="executionLifecyclePluginEnabled_${prkey}">
                                            <g:render template="/framework/renderPluginDesc" model="${[
                                                    serviceName    : ServiceNameConstants.ExecutionLifecycle,
                                                    description    : pluginDescription,
                                                    showPluginIcon : true,
                                                    showNodeIcon   : false,
                                                    hideTitle      : false,
                                                    hideDescription: false,
                                                    fullDescription: true
                                            ]}"/>
                                        </label>
                                    </div>

                                </div>
                            </div>


                            <g:if test="${pluginDescription?.properties}">
                                <g:set var="prefix" value="executionLifecyclePlugins.${pluginKey}.configMap."/>
                                <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                                        service:ServiceNameConstants.ExecutionLifecycle,
                                        provider:pluginDescription.name,
                                        properties:pluginDescription?.properties,
                                        report: params.executionLifecyclePluginValidation?.get(pluginType),
                                        prefix:prefix,
                                        values:pluginConfig?:[:],
                                        fieldnamePrefix:prefix,
                                        origfieldnamePrefix:'orig.' + prefix,
                                        allowedScope:com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance
                                ]}"/>
                            </g:if>
                        </g:if>
                    </div>
                </g:each>
            </div>
        </div>
    </g:if>
</feature:enabled>

  %{--Log level--}%
  <div class="tab-pane" id="tab_other">
  <section class="section-space-lg">
      <div class="form-group">
          <label class="${labelColClass}" for="loglevel">
            <g:message code="scheduledExecution.property.loglevel.label" />
          </label>
          <div class="${fieldColSize}">
            <div class="radio radio-inline">
                <g:radio id="log-level-normal" name="loglevel" value="INFO" checked="${scheduledExecution?.loglevel != 'DEBUG'}"/>
                <label for="log-level-normal">
                    <g:message code="loglevel.normal" />
                </label>
            </div>
            <div class="radio radio-inline">
              <g:radio id="log-level-debug" name="loglevel" value="DEBUG" checked="${scheduledExecution?.loglevel == 'DEBUG'}"/>
              <label for="log-level-debug">
                  <g:message code="loglevel.debug" />
              </label>
            </div>
            <div class="help-block">
                <g:message code="scheduledExecution.property.loglevel.help" />
            </div>
          </div>
      </div>

      %{--multiple exec--}%
      <div class="form-group">
          <div class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.multipleExecutions.label"/>
          </div>

          <div class="${fieldColSize}">
            <div class="radio radio-inline">
              <g:radio value="false" name="multipleExecutions"
                       checked="${!scheduledExecution.multipleExecutions}"
                       id="multipleFalse"/>
              <label for="multipleFalse">
                 <g:message code="no"/>
              </label>
            </div>
            <div class="radio radio-inline">
              <g:radio name="multipleExecutions" value="true"
                       checked="${scheduledExecution.multipleExecutions}"
                       id="multipleTrue"/>
              <label for="multipleTrue">
                  <g:message code="yes"/>
              </label>
            </div>
            <div class="help-block">
                  <g:message code="scheduledExecution.property.multipleExecutions.description"/>
            </div>
          </div>
      </div>
      %{--Job timeout--}%
      <div class="form-group">
          <div class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.maxMultipleExecutions.label"/>
          </div>

          <div class="${fieldColHalfSize}">

              <input type='text' name="maxMultipleExecutions" value="${enc(attr:scheduledExecution?.maxMultipleExecutions)}"
                     id="maxMultipleExecutions" class="form-control"/>

              <span class="help-block">
                  <g:message code="scheduledExecution.property.maxMultipleExecutions.description"/>
              </span>
          </div>
      </div>
      %{--Job timeout--}%
      <div class="form-group">
          <div class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.timeout.label" default="Timeout"/>
          </div>

          <div class="${fieldColHalfSize}">

              <input type='text' name="timeout" value="${enc(attr:scheduledExecution?.timeout)}"
                     id="schedJobTimeout" class="form-control"/>

              <span class="help-block">
                  <g:message code="scheduledExecution.property.timeout.description"/>
              </span>
          </div>
      </div>
      %{--Job retry--}%
      <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'retry', 'has-error')}">
          <div class="${labelColSize} control-label">
              <label for="schedJobRetry"><g:message code="scheduledExecution.property.retry.label" default="Retry"/></label>
          </div>

          <div class="${fieldColShortSize}">

              <input type='text' name="retry" value="${enc(attr:scheduledExecution?.retry)}"
                     id="schedJobRetry" class="form-control"/>
              <g:hasErrors bean="${scheduledExecution}" field="retry">

                  <div class="text-danger">
                      <g:renderErrors bean="${scheduledExecution}" as="list" field="retry"/>
                  </div>
              </g:hasErrors>
              <span class="help-block">
                  <g:message code="scheduledExecution.property.retry.description"/>
              </span>
          </div>

          <label class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.retry.delay.label" default="Timeout"/>
          </label>

          <div class="${fieldColShortSize}">

              <input type='text' name="retryDelay" value="${enc(attr:scheduledExecution?.retryDelay)}"
                     id="schedJobRetryDelay" class="form-control"/>


              <span class="help-block">
                  <g:message code="scheduledExecution.property.retry.delay.description"/>
              </span>
          </div>
      </div>
      %{--log limit--}%
      <div class="form-group">
          <label class="${labelColSize} control-label text-form-label" for="schedJobLogOutputThreshold">
              <g:message code="scheduledExecution.property.logOutputThreshold.label" default="Output Limit"/>
          </label>

          <div class="${fieldColShortSize}">

              <input type='text' name="logOutputThreshold" value="${enc(attr: scheduledExecution?.logOutputThreshold)}"
                     id="schedJobLogOutputThreshold" class="form-control"
                     placeholder="${message(code:"scheduledExecution.property.logOutputThreshold.placeholder")}"/>

              <span class="help-block">
                  <g:message code="scheduledExecution.property.logOutputThreshold.description" default=""/>
              </span>
          </div>
          <label class="${labelColSize} control-label text-form-label" for="logOutputThresholdAction">
              <g:message code="scheduledExecution.property.logOutputThresholdAction.label" default="Action"/>
          </label>

          <div class="${fieldColShortSize}">
              <div class="radio">
                <g:radio id="logOutputThresholdAction" name="logOutputThresholdAction" value="halt" checked="${!scheduledExecution?.logOutputThresholdAction || scheduledExecution?.logOutputThresholdAction=='halt'}"/>
                <label for="logOutputThresholdAction" title="${message(code: "scheduledExecution.property.logOutputThresholdAction.halt.description")}">
                    <g:message code="scheduledExecution.property.logOutputThresholdAction.halt.label"/>
                </label>
              </div>

              <div class="input-group">
                  <g:helpTooltip code="scheduledExecution.property.logOutputThresholdAction.halt.description" placement="left"/>
              <input type='text' name="logOutputThresholdStatus" value="${enc(attr: scheduledExecution?.logOutputThresholdStatus)}"
                         id="schedJobLogOutputThresholdStatus" class="form-control"
                         placeholder="${message(code:"scheduledExecution.property.logOutputThresholdStatus.placeholder")}"/>
              </div>
              <div class="radio">
                <g:radio id="logOutputThresholdActionTruncateAndContinue" name="logOutputThresholdAction" value="truncate" checked="${scheduledExecution?.logOutputThresholdAction=='truncate'}"/>
                <label for="logOutputThresholdActionTruncateAndContinue" title="${message(code: "scheduledExecution.property.logOutputThresholdAction.truncate.description")}">
                  <g:message code="scheduledExecution.property.logOutputThresholdAction.truncate.label"/>
                </label>
              </div>
              <span class="help-block">
                  <g:message code="scheduledExecution.property.logOutputThresholdAction.description" default=""/>
              </span>
          </div>
      </div>

      %{--default exec tab--}%
      <div class="form-group">
          <div class="${labelColSize} control-label text-form-label">
              <g:message code="scheduledExecution.property.defaultTab.label"/>
          </div>

          <div class="${fieldColSize}">
              <div class="radio radio-inline">
                <g:radio value="nodes" name="defaultTab"
                         checked="${!scheduledExecution.defaultTab || scheduledExecution.defaultTab in ['summary','monitor','nodes']}"
                         id="tabSummary"/>
                <label for="tabSummary">
                    <g:message code="execution.page.show.tab.Nodes.title"/>
                </label>
              </div>

              <div class="radio radio-inline">
                <g:radio name="defaultTab" value="output"
                         checked="${scheduledExecution.defaultTab=='output'}"
                         id="tabOutput"/>
                <label for="tabOutput">
                    <g:message code="execution.show.mode.Log.title"/>
                </label>
              </div>
              <div class="radio radio-inline">
                <g:radio name="defaultTab" value="html"
                         checked="${scheduledExecution.defaultTab=='html'}"
                         id="tabHTML"/>
                <label for="tabHTML">
                    <g:message code="html"/>
                </label>
              </div>



              <span class="help-block">
                  <g:message code="scheduledExecution.property.defaultTab.description"/>
              </span>
          </div>
      </div>

      %{--uuid--}%
      <div class="form-group ${hasErrors(bean: scheduledExecution, field: 'uuid', 'has-error')}" id="schedJobUuidLabel">
          <label for="schedJobUuid" class=" ${enc(attr:labelColClass)} text-primary">
              <g:message code="uuid" />
          </label>

          <div class="${fieldColSize}">
              <g:if test="${editSchedExecId && scheduledExecution?.uuid}">
                  <p class="form-control-static text-primary" title="${g.message(code:'uuid.for.this.job')}">
                      <g:enc>${scheduledExecution?.uuid}</g:enc>
                  </p>
              </g:if>
              <g:else>
                  <input type='text' name="uuid" value="${enc(attr:scheduledExecution?.uuid)}"
                         id="schedJobUuid" size="36" class="form-control"/>
                  <g:hasErrors bean="${scheduledExecution}" field="uuid">
                      <i class="glyphicon glyphicon-warning-sign" id="schedJobUuidErr"></i>
                      <wdgt:eventHandler for="schedJobUuid" state="unempty" frequency="1">
                          <wdgt:action target="schedJobUuidLabel" removeClassname="has-error"/>
                          <wdgt:action visible="false" target="schedJobUuidErr"/>
                      </wdgt:eventHandler>
                  </g:hasErrors>
              </g:else>
          </div>
      </div>
  </section>%{--//Log level--}%
</div><!-- end#tab_other -->

<script type="text/javascript">
//<!CDATA[
        var selFrameworkProject='${enc(js:project)}';
        var selArgs='${enc(js:scheduledExecution?.argString)}';
var curSEID ='${enc(js:editSchedExecId?:"")}';
function getCurSEID(){
    return curSEID;
}




        var wascancelled=false;
        function jobEditCancelled(){
            wascancelled=true;
        }
        /**
         * Validate the form
         *
         */
         function validateJobEditForm(form){
             var wfitem=jQuery(form).find('div.wfitemEditForm');
             let valid=true
             if(wfitem.length && !wascancelled){
                 jobeditor.addError('workflow');
                 wfitem.addClass('alert-warning')
                 if (wfitem.find("span.cancelsavemsg").length) {
                     wfitem.find("span.cancelsavemsg").show();
                 }
                 valid= false;
             }
            var optedit= jQuery(form).find('div.optEditForm');
            if (optedit.length && !wascancelled) {
                jobeditor.addError('option');
                optedit.addClass('alert-warning')
                if(optedit.find("span.cancelsavemsg").length){
                    optedit.find("span.cancelsavemsg").show();
                }
                valid= false;
            }
             return valid;
         }
        function _updateBoxInfo(name, data) {

        }
        function setupUndoRedoControls(){
            jQuery('.undoredocontrols').on('click','.act_undo',function(e){
                _doUndoAction(jQuery(e.target).data('undo-key'));
            }).on('click','.act_redo',function(e){
                _doRedoAction(jQuery(e.target).data('undo-key'));
            }).on('click','.act_revert_popover',function(e){
                _initPopoverContentRef("#undoredo"+ jQuery(e.target).data('popover-key'));
                jQuery(e.target).popover('show');
            });
            jQuery('body').on('click','.act_revert_cancel',function(e){
                jQuery('#revertall_'+ jQuery(e.target).data('popover-key')).popover('hide');
            }).on('click','.act_revert_confirm',function(e){
                jQuery('#revertall_'+jQuery(e.target).data('popover-key')).popover('destroy');
                _doRevertAction(jQuery(e.target).data('undo-key'));
            });
        }
        var nodeFilter;
        var nodeFilterMap = {};
        function registerNodeFilters(obj,key){
            nodeFilterMap[key]=obj;
        }
        function handleNodeFilterLink(link){
            var holder = jQuery(link).parents('.node_filter_link_holder');
            var nflinkid=holder.data('node-filter-link-id');
            var nflinkid2=holder.attr('id');
            if(nflinkid && nodeFilterMap[nflinkid]){
                nodeFilterMap[nflinkid].selectNodeFilterLink(link);
            }else if(nflinkid2 && nodeFilterMap['#'+nflinkid2]){
                nodeFilterMap['#'+nflinkid2].selectNodeFilterLink(link);
            }else{
                nodeFilter.selectNodeFilterLink(link);
            }
        }
        function handleNodeExcludeFilterLink(link){
            var holder = jQuery(link).parents('.node_filter_link_holder');
            var nflinkid=holder.data('node-filter-link-id');
            var nflinkid2=holder.attr('id');
            if(nflinkid && nodeFilterMap[nflinkid]){
                nodeFilterMap[nflinkid].selectNodeFilterExcludeLink(link);
            }else if(nflinkid2 && nodeFilterMap['#'+nflinkid2]){
                nodeFilterMap['#'+nflinkid2].selectNodeFilterExcludeLink(link);
            }else{
                nodeFilter.selectNodeFilterExcludeLink(link);
            }

        }
        function setupJobExecNodeFilterBinding(root,target,dataId){
            var filterParams = loadJsonData(dataId);
            var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
            var jobRefNodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.assign(filterParams, {
                        nodeSummary:nodeSummary,
                        nodefilterLinkId:root,
                        project: selFrameworkProject,
                        maxShown:20,
                        view: 'embed',
                        emptyMode: 'blank',
                        emptyMessage: "${g.message(code: 'JobExec.property.nodeFilter.null.description')}",
                        nodesTitleSingular: "${g.message(code: 'Node', default: 'Node')}",
                        nodesTitlePlural: "${g.message(code: 'Node.plural', default: 'Nodes')}"
                    })
            );
            ko.applyBindings(jobRefNodeFilter, jQuery(root)[0]);
            registerNodeFilters(jobRefNodeFilter,root);
        }
        function pageinit(){
            _enableWFDragdrop();

            setupUndoRedoControls();

            //define NodeFilters mvvm for the job
            var filterParams = loadJsonData('filterParamsJSON');
            var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
            nodeFilter = new NodeFilters(
                    appLinks.frameworkAdhoc,
                    appLinks.scheduledExecutionCreate,
                    appLinks.frameworkNodes,
                    Object.assign(filterParams, {
                        nodeSummary:nodeSummary,
                        maxShown:100,
                        nodefilterLinkId: '#nodegroupitem',
                         project: selFrameworkProject,
                         view:'embed',
                        nodesTitleSingular: "${g.message(code:'Node',default:'Node')}",
                        nodesTitlePlural: "${g.message(code:'Node.plural',default:'Nodes')}"
                    })
            );
            ko.applyBindings(nodeFilter,jQuery('#nodegroupitem')[0]);
            registerNodeFilters(nodeFilter, '#nodegroupitem');
            nodeSummary.reload();
            nodeFilter.updateMatchedNodes();
            jQuery('body').on('click', '.nodefilterlink', function (evt) {
                evt.preventDefault();
                handleNodeFilterLink(this);
            })
            jQuery('body').on('click', '.nodeexcludefilterlink', function (evt) {
                evt.preventDefault();
                handleNodeExcludeFilterLink(this);
            })
            .on('change','.node_dispatch_radio',function(evt){
                nodeFilter.updateMatchedNodes();
            })
            ;
            const jobDef = loadJsonData('jobDefinitionJSON')
            var jobeditor = new JobEditor(jobDef)
            window.jobeditor = jobeditor
            window._jobeditor = jobeditor
<g:if test="${params.executionLifecyclePluginValidation}">
            jobeditor.addError('plugins');
</g:if>
            initKoBind(null,{jobeditor:jobeditor})
        }

        jQuery(pageinit);
//]>
</script>

<g:javascript>
    if (typeof(_initPopoverContentRef) == 'function') {
        _initPopoverContentRef();
    }
</g:javascript>
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace-bundle.js"/><!--<![endif]-->
<!--[if (gt IE 8)|!(IE)]><!--> <asset:javascript src="ace/ext-language_tools.js"/><!--<![endif]-->
<div id="msg"></div>
    <g:render template="/framework/storageBrowseModalKO"/>

