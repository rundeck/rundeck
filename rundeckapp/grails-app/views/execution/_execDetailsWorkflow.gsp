<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
   _execDetailsWorkflow.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Apr 20, 2010 2:53:14 PM
   $Id$
--%>
<g:set var="rkey" value="${g.rkey()}"/>
<g:unless test="${isAdhoc}">
<g:if test="${edit}">
<div>
    <span class=""><g:message code="Workflow.property.keepgoing.prompt" /></span>
    <label>
        <input type="radio" name="workflow.keepgoing" value="false" ${workflow?.keepgoing?'':'checked'}/>
        <g:message code="Workflow.property.keepgoing.false.description"/>
    </label>
    <label>
        <input type="radio" name="workflow.keepgoing" value="true" ${workflow?.keepgoing?'checked':''}/>
        <g:message code="Workflow.property.keepgoing.true.description"/>
    </label>
</div>
<div>

    <span class="" title="Strategy for iteration"><g:message code="strategy" />:</span>
        <label title="Execute the full workflow on each node before the next node">
            <input id="wf_strat_node_first" type="radio" name="workflow.strategy" value="node-first" ${!workflow?.strategy||workflow?.strategy=='node-first'?'checked':''}/>
            <g:message code="Workflow.strategy.label.node-first"/>
        </label>
        <label title="Execute each step on all nodes before the next step">
            <input type="radio" name="workflow.strategy" value="step-first" ${workflow?.strategy=='step-first'?'checked':''}/>
            <g:message code="Workflow.strategy.label.step-first"/>
        </label>

        <span class=" action obs_tooltip" id="nodeStratHelp"><g:img file="icon-small-help.png" width="16px" height="16px"/> Explain </span>
        <div class="popout tooltipcontent" id="nodeStratHelp_tooltip" style="display:none; background:white; position:absolute;">
            <style type="text/css">
                td.nodea{
                    color:blue;
                }
                td.nodeb{
                    color:green;
                }
                
            </style>
            <table>
                <tr>
                    <td width="200px;">

                <span class="info note">Node-oriented: <g:message code="Workflow.strategy.description.node-first"/></span>
                    </td>
                    <td width="200px;"><span class="info note">Step-oriented: <g:message code="Workflow.strategy.description.step-first" /></span></td>
                </tr>
                <tr>
                <td>
                <table>
                    <tr><td>1.</td><td class="nodea">NodeA</td> <td>step 1</td></tr>
                    <tr><td>2.</td><td class="nodea">"</td> <td>step 2</td></tr>
                    <tr><td>3.</td><td class="nodea">"</td> <td>step 3</td></tr>
                    <tr><td>4.</td><td class="nodeb">NodeB</td> <td>step 1</td></tr>
                    <tr><td>5.</td><td class="nodeb">"</td> <td>step 2</td></tr>
                    <tr><td>6.</td><td class="nodeb">"</td> <td>step 3</td></tr>
                </table>

            </td>

            <td>

            <table>
                <tr><td>1.</td><td class="nodea">NodeA</td> <td class="step1">step 1</td></tr>
                <tr><td>2.</td><td class="nodeb">NodeB</td> <td class="step1">"</td></tr>
                <tr><td>3.</td><td class="nodea">NodeA</td> <td class="step2">step 2</td></tr>
                <tr><td>4.</td><td class="nodeb">NodeB</td> <td class="step2">"</td></tr>
                <tr><td>5.</td><td class="nodea">NodeA</td> <td>step 3</td></tr>
                <tr><td>6.</td><td class="nodeb">NodeB</td> <td>"</td></tr>
            </table>
            </td></tr></table>
        </div>
        <g:javascript>
            fireWhenReady('nodeStratHelp', initTooltipForElements.curry('.obs_tooltip'));
        </g:javascript>
%{--<span class="label">threadcount:</span> ${workflow?.threadcount}--}%
</div>
</g:if>
</g:unless>
<div class="pflowlist ${edit?'edit':''} rounded ${isAdhoc?'adhoc':''}" style="">
    <g:if test="${edit}">
        <div id="wfundoredo" >
            <g:render template="/common/undoRedoControls"/>
        </div>
    </g:if>
    <ol id="wfilist_${rkey}" class="flowlist">
        <g:render template="/execution/wflistContent" model="${[workflow:workflow,edit:edit,noimgs:noimgs,project:project]}"/>
    </ol>
    <div id="workflowDropfinal" wfitemNum="${workflow?.commands? workflow.commands.size():0}" style="display:none"></div>
    <div class="empty note ${error?'error':''}" id="wfempty" style="${wdgt.styleVisible(unless:workflow && workflow?.commands)}">
        No Workflow ${g.message(code:'Workflow.step.label')}s
    </div>
    <g:if test="${edit}">
    <div >
    <div id="wfnewbutton" style="margin-top:5px;">
        <span class="btn btn-default btn-sm ready" onclick="$('wfnewtypes').show();$('wfnewbutton').hide();" title="Add a new Workflow ${g.message(code:'Workflow.step.label')} to the end">
            <b class="glyphicon glyphicon-plus"></b>
            Add a ${g.message(code:'Workflow.step.label')}
        </span>
    </div>
    <div id="wfnewtypes" style="display:none; margin-top:10px;" class="panel panel-success">
        <g:render template="/execution/wfAddStep"
            model="[addMessage:'Workflow.step.label.add',chooseMessage:'Workflow.step.label.choose.the.type']"
        />
    </div>

    <div id="wfnew_eh_types" style="display:none;  margin-top:10px;" class="panel panel-success">
        %{--This element is moved around to show the add error-handle buttons for a step--}%
        <g:render template="/execution/wfAddStep"
                model="[addMessage:'Workflow.stepErrorHandler.label.add',descriptionMessage:'Workflow.stepErrorHandler.description',chooseMessage:'Workflow.stepErrorHandler.label.choose.the.type']"
        />
    </div>
    </div>
        <script type="text/javascript">
            fireWhenReady('wfnew_eh_types',function(){
                $('wfnew_eh_types').select('.add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHChooseType);
                });
                $('wfnew_eh_types').select('.add_node_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHNodeStepType);
                });
                $('wfnew_eh_types').select('.cancel_add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewEHCancel);
                });
            })
            fireWhenReady('wfnewtypes', function () {
                $('wfnewtypes').select('.add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewStepChooseType);
                });
                $('wfnewtypes').select('.add_node_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewNodeStepChooseType);
                });
                $('wfnewtypes').select('.cancel_add_step_type').each(function (e) {
                    Event.observe(e, 'click', _evtNewStepCancel);
                });
            })
        </script>
</g:if>

</div>
<g:if test="${!edit && !isAdhoc}">
    <div>
    <span class="text-muted text-em">
        <g:message code="Workflow.property.keepgoing.prompt"/>
        <strong><g:message
            code="Workflow.property.keepgoing.${workflow?.keepgoing ? true : false}.description"/></strong>
    </span>
    </div>
    <div>
    <span class="text-muted text-em">
        <g:message code="strategy"/>:
        <strong><g:message code="Workflow.strategy.description.${workflow?.strategy}"/></strong>
    </span>

    </div>
</g:if>
<div class="clear"></div>




<div id="wfnewitem"></div>
