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
<%--
   _editProjectForm.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: 8/1/11 11:38 AM
--%>

<%@ page import="rundeck.UtilityTagLib; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %>
<g:render template="/common/messages" model="[notDismissable:true]"/>
<script type="text/javascript">
    function changeCronExpression(elem){
        clearHtml($('crontooltip'));
        var params={crontabString:$F(elem)};
        new Ajax.Updater('cronstrinfo',
            '${createLink(controller:'scheduledExecution',action:'checkCrontab')}',{
                parameters:params,
                evalScripts:true
            }
        );
    }
    var cronSects=['Second','Minute','Hour','Day of Month','Month','Day of Week','Year'];
    function tkeyup(el){
        clearHtml('cronstrinfo');
        var pos=getCaretPos(el);
        var f =$F(el);
        //find # of space chars prior to pos
        var sub=f.substring(0,pos);
        var c = sub.split(' ').size();
        if(c>=1&&c<=7){
            setText($('crontooltip'),cronSects[c-1]);
        }else{
            clearHtml('crontooltip');
        }
    }
    function getCaretPos(el) {
        var rng, ii = -1;
        if (typeof el.selectionStart == "number") {
            ii = el.selectionStart;
        } else if (document.selection && el.createTextRange) {
            rng = document.selection.createRange();
            rng.collapse(true);
            rng.moveStart("character", -el.value.length);
            ii = rng.text.length;
        }
        return ii;
    }
    function cleanerchkbox(el) {
        if(el.checked){
            $('cleaner_config').show()
        } else {
            $('cleaner_config').hide()
        }
    }
</script>
<div class="list-group">
  <g:if test="${editOnly}">
    <g:hiddenField name="project" value="${project}"/>
  </g:if>
  <g:if test="${!editOnly}">
    <div class="list-group-item">
      <div class="form-group ${projectNameError?'has-error':''}">
        <label for="project" class="required">
          <g:message code="domain.Project.field.name" default="Project Name"/>
        </label>
        <g:textField name="newproject" size="50" autofocus="true" value="${newproject}" class="form-control"/>
        <g:if test="${projectNameError}">
          <div class="text-warning"><g:enc>${projectNameError}</g:enc></div>
        </g:if>
      </div>
    </div>
  </g:if>
  <div class="list-group-item">
    <div class="form-group ">
        <label for="label">
            <g:message code="domain.Project.label.label" default="Label"/>
        </label>
        <g:textField name="label" size="50"  value="${projectLabel}" class="form-control"/>
    </div>
    <div class="form-group ">
        <label for="description">
            <g:message code="domain.Project.description.label" default="Description"/>
        </label>
        <g:textField name="description" size="50"  value="${projectDescription}" class="form-control"/>
        <g:if test="${projectDescriptionError}">
            <div class="text-warning"><g:enc>${projectDescriptionError}</g:enc></div>
        </g:if>
    </div>
  </div>
<feature:enabled name="cleanExecutionsHistoryJob">
  <div class="list-group-item">
      <label class=" control-label"><g:message code="execution.history.clean.label"/>:</label>
      <div class="row">
          <div class="col-sm-4">
              <g:set var="isSelected" value="${enableCleanHistory}"/>
              <div class="checkbox">
                  <g:checkBox
                          name="cleanerHistory"
                          value="${enableCleanHistory}"
                          class="fcopy"
                          id="${nkey+'enable_cleaner_input'}"
                          onchange='cleanerchkbox(this)'
                          checked="${isSelected}"/>
                  <label>
                      <b><g:enc>Enable</g:enc></b>
                  </label>
                  <span class="help-block"><g:enc>Enable cleaner executions history</g:enc></span>
              </div>
          </div>
      </div>
    <div id="cleaner_config" style="display: ${isSelected ? 'block' : 'none' }">
        <div class="form-group ${cleanerHistoryConfigError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="domain.Project.days.to.clean.execution" default="Days to keep executions"/>
            </label>
            <g:field name="cleanperiod" type="number" size="50"  value="${cleanerHistoryPeriod}" class="form-control"/>
            <g:if test="${cleanerHistoryConfigError}">
                <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group ${cleanerHistoryConfigError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="domain.Project.minimum.to.keep.execution" default="Minimum executions to keep"/>
            </label>
            <g:field name="minimumtokeep" type="number" size="50"  value="${minimumExecutionToKeep}" class="form-control"/>
            <g:if test="${cleanerHistoryConfigError}">
                <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group ${cleanerHistoryConfigError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="domain.Project.maximum.size.deletion.execution" default="Maximum size of the deletion"/>
            </label>
            <g:field name="maximumdeletionsize" type="number" size="50"  value="${maximumDeletionSize}" class="form-control"/>
            <g:if test="${cleanerHistoryConfigError}">
                <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group">
            %{--<div class="panel panel-default panel-tab-content crontab tabtarget"  >--}%
            <div class="${labelColSize}  control-label text-form-label">
                <g:message code="domain.Project.schedule.clean.execution" default="Schedule clean history job (Cron expression)"/>
            </div>
            <div class="row">
                <div class="col-sm-12">
                    <div  class="form-group">
                        <g:textField name="crontabString"
                                     value="${cronExression}"
                                     onchange="changeCronExpression(this);"
                                     onblur="changeCronExpression(this);"
                                     onkeyup='tkeyup(this);'
                                     onclick='tkeyup(this);'
                                     class="form-control input-sm"
                                     size="50"/>
                        <g:if test="${cleanerHistoryConfigError}">
                            <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
                        </g:if>

                    </div>
                </div>
                <div class="col-sm-4">
                    <span id="crontooltip" class="label label-info form-control-static" style="padding-top:10px;"></span>
                </div>
                <span id="cronstrinfo"></span>

            </div>
            <div class="row">
                <div class="text-primary col-sm-12">
                    <div>
                        Ranges: <code>1-3</code>.  Lists: <code>1,4,6</code>. Increments: <code>0/15</code> "every 15 units starting at 0".
                    </div>
                    See: <a href="${g.message(code:'documentation.reference.cron.url')}" class="external" target="_blank">Cron reference</a> for formatting help
                </div>
            </div>
        </div>
    </div>
  </div>
</feature:enabled>
  <g:set var="categories" value="${new HashSet(extraConfig?.values()?.collect { it.configurable.categories?.values() }.flatten())}"/>
  <g:each in="${categories.sort() - 'resourceModelSource'}" var="category">
    <div class="list-group-item">
    <g:render template="projectConfigurableForm"
              model="${[extraConfigSet: extraConfig?.values(),
                          category      : category,
                          categoryPrefix     : 'extra.category.' + category + '.',
                          titleCode     : 'project.configuration.extra.category.' + category + '.title',
                          helpCode      : 'project.configuration.extra.category.' + category + '.description'
              ]}"/>
    </div>
  </g:each>
  <g:if test="${nodeExecDescriptions}">
    <div class="list-group-item">
      <span class="h4">Default <g:message code="framework.service.NodeExecutor.label" /></span>
      <span class="help-block"><g:message code="domain.Project.edit.NodeExecutor.explanation" /></span>
      <g:each in="${nodeExecDescriptions}" var="description" status="nex">
        <g:set var="nkey" value="${g.rkey()}"/>
        <g:set var="isSelected" value="${defaultNodeExec == description.name}"/>
        <div class="radio">
          <g:radio
            name="defaultNodeExec"
            value="${nex}"
            class="nexec"
            id="${nkey+'_input'}"
            checked="${isSelected}"/>
            <label>
              <b><g:enc>${description.title}</g:enc></b>
            </label>
            <span class="help-block"><g:enc>${description.description}</g:enc></span>
        </div>
        <g:hiddenField name="nodeexec.${nex}.type" value="${description.name}"/>
        <g:set var="nodeexecprefix" value="nodeexec.${nex}.config."/>
        <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
          <wdgt:action visible="false" targetSelector=".nexecDetails"/>
        </wdgt:eventHandler>
        <g:if test="${description && description.properties}">
          <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
            <wdgt:action visible="true" target="${nkey+'_det'}"/>
          </wdgt:eventHandler>
          <div class="well well-sm nexecDetails" id="${enc(attr:nkey) + '_det'}" style="${wdgt.styleVisible(if: isSelected)}">
            <div class="form-horizontal " >
              <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                        service:com.dtolabs.rundeck.plugins.ServiceNameConstants.NodeExecutor,
                        provider:description.name,
                        properties:description.properties,
                        report:nodeexecreport?.errors && isSelected ? nodeexecreport : null,
                        prefix:nodeexecprefix,
                        values:isSelected ? nodeexecconfig : null,
                        fieldnamePrefix:nodeexecprefix,
                        origfieldnamePrefix:'orig.' + nodeexecprefix,
                        allowedScope: PropertyScope.Project
              ]}"/>
            </div>
          </div>
        </g:if>
      </g:each>
    </div>
  </g:if>
  <g:if test="${fileCopyDescriptions}">
    <div class="list-group-item">
    <span class="h4">Default Node <g:message code="framework.service.FileCopier.label"/></span>
    <span class="help-block"><g:message code="domain.Project.edit.FileCopier.explanation" /></span>
    <g:each in="${fileCopyDescriptions}" var="description" status="nex">
      <g:set var="nkey" value="${g.rkey()}"/>
      <g:set var="isSelected" value="${defaultFileCopy == description.name}"/>
      <div class="radio">
        <g:radio
            name="defaultFileCopy"
            value="${nex}"
            class="fcopy"
            id="${nkey+'_input'}"
            checked="${isSelected}"/>
        <label>
          <b><g:enc>${description.title}</g:enc></b>
        </label>
        <span class="help-block"><g:enc>${description.description}</g:enc></span>
      </div>
      <g:hiddenField name="fcopy.${nex}.type" value="${description.name}"/>
      <g:set var="fcopyprefix" value="fcopy.${nex}.config."/>
      <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
          <wdgt:action visible="false" targetSelector=".fcopyDetails"/>
      </wdgt:eventHandler>
      <g:if test="${description && description.properties}">
        <wdgt:eventHandler state="checked" for="${nkey+'_input'}">
          <wdgt:action visible="true" target="${nkey+'_det'}"/>
        </wdgt:eventHandler>
        <div class="well well-sm fcopyDetails" id="${enc(attr:nkey) + '_det'}" style="${wdgt.styleVisible(if: isSelected)}">
          <div class="form-horizontal">
            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                      service:com.dtolabs.rundeck.plugins.ServiceNameConstants.FileCopier,
                      provider:description.name,
                      properties:description.properties,
                      report:fcopyreport?.errors && isSelected ? fcopyreport : null,
                      prefix:fcopyprefix,
                      values:isSelected?fcopyconfig:null,
                      fieldnamePrefix:fcopyprefix,
                      origfieldnamePrefix:'orig.'+fcopyprefix,
                      allowedScope:PropertyScope.Project
            ]}"/>
          </div>
        </div>
      </g:if>
    </g:each>
    </div>
  </g:if>
</div>
