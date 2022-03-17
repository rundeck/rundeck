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

<%@ page import="com.dtolabs.rundeck.plugins.ServiceNameConstants; rundeck.UtilityTagLib; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope" contentType="text/html;charset=UTF-8" %>
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
  <g:if test="${editOnly}">
    <g:hiddenField name="project" value="${project}"/>
  </g:if>
<div class="tab-pane active" id="tab_details">
  <g:if test="${!editOnly}">
      <div class="form-group ${projectNameError?'has-error':''}">
        <label for="project" class="required">
          <g:message code="domain.Project.field.name" default="Project Name"/>
        </label>
          <g:textField name="newproject" size="50" autofocus="true" value="${newproject}" class="form-control"
                       data-bind="value: name"/>
        <g:if test="${projectNameError}">
          <div class="text-warning"><g:enc>${projectNameError}</g:enc></div>
        </g:if>
      </div>
  </g:if>
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
  <div class="tab-pane" id="tab_history">
      <label class=" control-label"><g:message code="execution.history.cleanup.label" default="Execution History Clean"/>:</label>
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
                  <label for="${nkey+'enable_cleaner_input'}">
                      <b><g:enc>Enable</g:enc></b>
                  </label>
                  <span class="help-block"><g:enc>Enable cleaner executions history</g:enc></span>
              </div>
          </div>
      </div>
    <div id="cleaner_config" style="display: ${isSelected ? 'block' : 'none' }">
        <div class="form-group ${cleanerHistoryPeriodError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="execution.history.cleanup.retention.days" default="Days to keep executions. Default: 60"/>
            </label>
            <g:field name="cleanperiod" type="number" size="50" min="0" value="${cleanerHistoryPeriod}" class="form-control"/>
            <g:if test="${cleanerHistoryPeriodError}">
                <div class="text-warning"><g:enc>${cleanerHistoryPeriodError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group ${cleanerHistoryConfigError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="execution.history.cleanup.retention.minimum" default="Minimum executions to keep. Default: 50"/>
            </label>
            <g:field name="minimumtokeep" type="number" size="50" min="0" value="${minimumExecutionToKeep}" class="form-control"/>
            <g:if test="${cleanerHistoryConfigError}">
                <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group ${cleanerHistoryConfigError?'has-error':''}">
            <label for="cleanperiod">
                <g:message code="execution.history.cleanup.batch" default="Maximum size of the deletion. Default: 500"/>
            </label>
            <g:field name="maximumdeletionsize" type="number" size="50" min="0" value="${maximumDeletionSize}" class="form-control"/>
            <g:if test="${cleanerHistoryConfigError}">
                <div class="text-warning"><g:enc>${cleanerHistoryConfigError}</g:enc></div>
            </g:if>
        </div>
        <div class="form-group">
            %{--<div class="panel panel-default panel-tab-pane crontab tabtarget"  >--}%
            <div class="${labelColSize}  control-label text-form-label">
                <g:message code="execution.history.cleanup.schedule" default="Schedule clean history job (Cron expression). Default: 0 0 0 1/1 * ? * (Every days on 12:00 AM)"/>
            </div>
            <div class="row">
                <div class="col-sm-8">
                    <div  class="form-group">
                        <g:textField id="cronTextField"
                                     name="crontabString"
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
                <div class="col-sm-4 form-group">
                    <g:set var="propSelectValues" value="${cronModelValues.collect {k, v ->
                        [key: k, value: (cronModelValues[k] ?: it)]
                    }}"/>
                    <g:select name="${'example_cron_period_sel'}" from="${propSelectValues}" id="example_cron_period"
                              optionKey="key" optionValue="value"
                              noSelection="['':'-choose an example-']"
                              onchange="if(this.value){\$('cronTextField').value=this.value;}"
                              class="${formControlType} form-control"
                    />
                </div>
                <div class="col-sm-4">
                    <span id="crontooltip" class="label label-info form-control-static" style="padding-top:10px;"></span>
                </div>
                <span id="cronstrinfo"></span>

            </div>
            <div class="row">
                <div class="text-strong col-sm-12">
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
    <div class="tab-pane" id="tab_category_${category}">
    <g:render template="projectConfigurableForm"
              model="${[extraConfigSet: extraConfig?.values(),
                          category      : category,
                          categoryPrefix     : 'extra.category.' + category + '.',
                          titleCode     : 'project.configuration.extra.category.' + category + '.title',
                          helpCode      : 'project.configuration.extra.category.' + category + '.description'
              ]}"/>
    </div>
  </g:each>

<g:each in="${serviceDefaultsList}" var="serviceDefaults">

    <div class="tab-pane form-horizontal" id="tab_svc_${serviceDefaults.service}">

        <div class="form-group">
            <div class=" col-sm-12 help-block"><g:message
                    code="domain.Project.edit.${serviceDefaults.service}.explanation"/>
            </div>
        </div>

        <div class=" form-group spacing-lg">
            <div class="col-sm-12">
                <div class="btn-group btn-group-lg">
                    <button type="button" class="btn btn-primary dropdown-toggle" data-toggle="dropdown"
                            aria-haspopup="true" aria-expanded="false">

                        <span class="caret"></span>

                        <span data-bind="if: defaults['${serviceDefaults.service}'].type() ">

                            <span data-bind="if: defaults['${serviceDefaults.service}'].description() ">
                                <span data-bind="with: defaults['${serviceDefaults.service}'].description">
                                <!-- ko if: iconSrc -->
                                <img width="24px" height="24px" data-bind="attr: {src: iconSrc}"/>
                                <!-- /ko -->
                                <!-- ko if: glyphicon -->
                                <i data-bind="css: 'glyphicon glyphicon-'+glyphicon()"></i>
                                <!-- /ko -->
                                <!-- ko if: faicon -->
                                <i data-bind="css: 'fas fa-'+faicon()"></i>
                                <!-- /ko -->
                                <!-- ko if: fabicon -->
                                <i data-bind="css: 'fab fa-'+fabicon()"></i>
                                <!-- /ko -->
                                <!-- ko if: !iconSrc() && !glyphicon() && !faicon() && !fabicon() -->
                                <i class="rdicon icon-small plugin"></i>
                                <!-- /ko -->
                                <span data-bind="text: title"></span>
                                </span>
                            </span>
                        </span>
                        <span data-bind="if: !defaults['${serviceDefaults.service}'].type()">
                            Select a <g:message
                                    code="framework.service.${serviceDefaults.service}.label"/>
                        </span>

                        <span class="sr-only">Toggle Dropdown</span>
                    </button>


                    <ul class="dropdown-menu ">

                            <li data-bind="foreach: descriptions['${serviceDefaults.service}']">
                                <a href="#" data-bind="click: function(){$parent.defaults['${serviceDefaults.service}'].type(type())}">
                                    <!-- ko if: iconSrc -->
                                    <img width="16px" height="16px" data-bind="attr: {src: iconSrc}"/>
                                    <!-- /ko -->
                                    <!-- ko if: glyphicon -->
                                    <i data-bind="css: 'glyphicon glyphicon-'+glyphicon()"></i>
                                    <!-- /ko -->
                                    <!-- ko if: faicon -->
                                    <i data-bind="css: 'fas fa-'+faicon()"></i>
                                    <!-- /ko -->
                                    <!-- ko if: fabicon -->
                                    <i data-bind="css: 'fab fa-'+fabicon()"></i>
                                    <!-- /ko -->
                                    <!-- ko if: !iconSrc() && !glyphicon() && !faicon() && !fabicon() -->
                                    <i class="rdicon icon-small plugin"></i>
                                    <!-- /ko -->
                                    <span data-bind="text: title"></span>

                                </a>
                            </li>
                    </ul>
                </div>
                <span data-bind="if: defaults['${serviceDefaults.service}'].description() ">
                    <span data-bind="with: defaults['${serviceDefaults.service}'].description">
                        <span class="text-info">
                            <span data-bind="text: descriptionFirstLine"></span>
                        </span>
                    </span>
                </span>
                <input type="hidden" name="default_${serviceDefaults.service}" data-bind="value: defaults['${serviceDefaults.service}'].type"/>
                <input type="hidden" name="${serviceDefaults.prefix}.default.type" data-bind="value: defaults['${serviceDefaults.service}'].type"/>
            </div>
        </div>
        <g:each in="${serviceDefaults.descriptions}" var="description" status="nex">
            <g:set var="nkey" value="${g.rkey()}"/>
            <g:set var="isSelected" value="${serviceDefaults.selectedType == description.name}"/>

            <g:set var="fcopyprefix" value="${serviceDefaults.prefix}.default.config."/>
      <g:if test="${description && description.properties}">
          <g:set var="isSelected" value="${serviceDefaults.selectedType == description.name}"/>
          <div class=" " id="${enc(attr: nkey) + '_det'}"
               data-bind="if: defaults['${serviceDefaults.service}'].type()==='${enc(attr: description.name)}'">
              <hr/>

            <g:render template="/framework/pluginConfigPropertiesInputs" model="${[
                    service            : serviceDefaults.service,
                    provider           : description.name,
                    properties         : description.properties,
                    report             : serviceDefaults.errreport?.errors && isSelected ? serviceDefaults.errreport :
                                         null,
                    prefix             : fcopyprefix,
                    values             : isSelected ? serviceDefaults.config : null,
                    fieldnamePrefix    : fcopyprefix,
                    origfieldnamePrefix: 'orig.' + fcopyprefix,
                    allowedScope       : PropertyScope.Project
            ]}"/>

        </div>
      </g:if>
    </g:each>
    </div>

</g:each>

<div class="tab-pane form-horizontal" id="tab_plugins">

    <div class="form-group">
        <div class=" col-sm-12 help-block"><g:message
                code="domain.Project.edit.plugins.explanation" default="Project settings for Plugins"/>
        </div>
    </div>

    <div class=" form-group spacing-lg">
        <div class="col-sm-12">
            <plugin-set-config
                    class="project-config-plugins-vue"
                    :event-bus="EventBus"
                    service-name="PluginGroup"
                    :config-list="[]"
                    config-prefix="pluginValues.PluginGroup."
            ></plugin-set-config>
        </div>
    </div>
</div>