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
<div>
    <span class="label" title="Continue execution if any steps fail.">Keepgoing:</span>
    <g:if test="${edit}">
        <label><input type="radio" name="workflow.keepgoing" value="false" ${workflow?.keepgoing?'':'checked'}/> No</label>
        <label><input type="radio" name="workflow.keepgoing" value="true" ${workflow?.keepgoing?'checked':''}/> Yes</label>
    </g:if>
    <g:else>
        ${workflow?.keepgoing?true:false}
    </g:else>
    </div>
<div>
    <span class="label" title="Strategy for iteration">Strategy:</span>
    <g:if test="${edit}">
        <label title="Execute the full workflow on each node before the next node">
            <input type="radio" name="workflow.strategy" value="node-first" ${!workflow?.strategy||workflow?.strategy=='node-first'?'checked':''}/>
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

                <span class="info note">Node-oriented executes the full workflow on each each node before the next node</span>
                    </td>
                    <td width="200px;"><span class="info note">Step-oriented executes each step on all nodes before the next step</span></td>
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
    </g:if>
    <g:else>
        <g:message code="Workflow.strategy.label.${workflow?.strategy}"/>
    </g:else>

%{--<span class="label">threadcount:</span> ${workflow?.threadcount}--}%
</div>
<div class="pflowlist ${edit?'edit':''} rounded" style="${edit?'width:600px;':''}">
    <g:if test="${edit}">
        <div id="wfundoredo" >
            <div style="margin-bottom:10px;">
                <span class="button disabled small">Undo</span>
                <span class="button disabled small">Redo</span>
            </div>
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
    <div id="wfnewbutton" style="margin-top:5px; padding-left:20px;">
        <span class="action textbtn ready" onclick="$('wfnewtypes').show();$('wfnewbutton').hide();" title="Add a new Workflow ${g.message(code:'Workflow.step.label')} to the end">
            Add a ${g.message(code:'Workflow.step.label')}
        </span>
    </div>
    <div id="wfnewtypes" style="display:none; margin-top:10px" class="popout">
        <span > Add a Workflow ${g.message(code:'Workflow.step.label')}</span>
        <div class="info note">Choose the type of Workflow ${g.message(code:'Workflow.step.label')}:</div>
        <div style="margin:10px;">
        <span class="button action" onclick="_wfiaddnew('command');" title="Execute a remote command"><g:img file='icon-tiny-add.png'/> Command</span>
        <span class="button action" onclick="_wfiaddnew('script');" title="Execute  an inline script"><g:img file='icon-tiny-add.png'/> Script</span>
        <span class="button action" onclick="_wfiaddnew('scriptfile');" title="Execute a script file or URL"><g:img file='icon-tiny-add.png'/> Script file or URL</span>
        <span class="button action" onclick="_wfiaddnew('job');" title="Execute another Job"><g:img file='icon-tiny-add.png'/> Job Reference</span>
        </div>
        <div style="margin:10px; text-align:right;">
            <span class="action button small" onclick="$('wfnewtypes').hide();$('wfnewbutton').show();" title="Cancel adding new item"> Cancel</span>
        </div>
    </div>
    </div>
</g:if>
</div>
<div class="clear"></div>




<div id="wfnewitem"></div>
