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
<g:set var="ukey" value="${g.rkey()}" />
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


<g:set var="project" value="${scheduledExecution?.project ?: params.project?:request.project?: projects?.size() == 1 ? projects[0].name : ''}"/>
<g:embedJSON id="filterParamsJSON"
             data="${[filterName: params.filterName, matchedNodesMaxCount: matchedNodesMaxCount?:100, filter: scheduledExecution?.asFilter(),filterExcludeName: params.filterExcludeName, filterExclude: scheduledExecution?.asExcludeFilter(),nodeExcludePrecedence: scheduledExecution?.nodeExcludePrecedence, excludeFilterUncheck: scheduledExecution?.excludeFilterUncheck]}"/>
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
                     value="${!(cfg.getBoolean(config: 'gui.job.description.disableHTML', default: false))}"/>
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

      <g:render template="jobComponentProperties"
                model="[
                        jobComponents:jobComponents,
                        sectionName:'details',
                        jobComponentValues:jobComponentValues
                ]"
      />
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
                              jQuery('#wfnewtypes').show();
                              jQuery('#wfnewbutton').hide();
                          });
                      </g:javascript>
                  </g:if>
              </div>
          </div>
      </section>%{--//Workflow--}%

    <g:render template="jobComponentProperties"
              model="[
                      jobComponents:jobComponents,
                      sectionName:'workflow',
                      jobComponentValues:jobComponentValues
              ]"
    />
</div><!-- end#tab_workflow -->

  %{--Node Dispatch--}%
    <div class="tab-pane" id="tab_nodes">
  <section class="section-space-lg node_filter_link_holder" id="nodegroupitem">

      <div class="job-editor-resources-vue" id="job-editor-resources-vue">
          <resources-editor-section :event-bus="EventBus" />
      </div>

%{--//Node Dispatch--}%
  </section>
        <g:render template="jobComponentProperties"
                  model="[
                          jobComponents:jobComponents,
                          sectionName:'nodes',
                          jobComponentValues:jobComponentValues
                  ]"
        />
  </div><!-- end#tab_nodes-->

      %{--Notifications--}%
      <div class="tab-pane" id="tab_notifications"  >
      <section class="section-space-lg"  >
          <g:render template="editNotificationsForm" />


              <g:render template="jobComponentProperties"
                        model="[
                                jobComponents:jobComponents,
                                sectionName:'notifications',
                                jobComponentValues:jobComponentValues
                        ]"
              />
      </section>%{--//Notifications--}%
      </div><!-- end#tab_notifications -->

  %{--Schedule--}%
    <div class="tab-pane" id="tab_schedule">
  <section class="section-space-lg">

      <div class="job-editor-schedules-vue" id="job-editor-schedules-vue">
          <schedules-editor-section :event-bus="EventBus" :use-crontab-string="${scheduledExecution?.shouldUseCrontabString()?:false}"/>
      </div>

      <g:render template="jobComponentProperties"
                model="[
                        jobComponents:jobComponents,
                        sectionName:'schedule',
                        jobComponentValues:jobComponentValues
                ]"
      />
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
      <div class="job-editor-other-vue" id="job-editor-other-vue">
          <other-editor-section :event-bus="EventBus" />
      </div>
      <g:render template="jobComponentProperties"
                model="[
                        jobComponents:jobComponents,
                        sectionName:'other',
                        jobComponentValues:jobComponentValues
                ]"
      />
  </section>%{--//Log level--}%
</div><!-- end#tab_other -->

%{-- begin: iterate through extra sections not already defined --}%
<g:set var="componentSections" value="${g.
        jobComponentSections(
                jobComponents: jobComponents,
                defaultSection: 'other',
                skipSections: ['details', 'workflow', 'nodes', 'schedule', 'notifications', 'other']
        )}"/>

<g:each var="sectionName" in="${componentSections.keySet()?.sort()}">
    <div class="tab-pane" id="tab_${enc(attr:sectionName)}">
        <section class="section-space-lg">

            <g:render template="jobComponentProperties"
                model="[
                        jobComponents:jobComponents,
                        sectionName:sectionName,
                        jobComponentValues:jobComponentValues
                ]"
            />

        </section>
    </div>
</g:each>
%{-- end extra job component ui sections --}%

%{-- begin: embed json data of component validation --}%
<g:embedJSON id="jobComponentValidation"
             data="${jobComponentValidation?.collectEntries { [it.key, it.value.errors] } ?: [:]}"/>
%{-- end: json component validation data --}%

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
                        maxShown:filterParams.matchedNodesMaxCount,
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

            jQuery('#tab_execution_plugins .textarea.form-control.apply_ace').each(function () {
                _setupAceTextareaEditor(this);
            });
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

