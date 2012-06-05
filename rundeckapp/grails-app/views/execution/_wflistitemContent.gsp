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
    _wflistitemContent.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jul 27, 2010 3:53:40 PM
    $Id$


 --%><div id="wfivis_${i}" style="${wdgt.styleVisible(unless:i==highlight)}">
    <div class="pflowitem wfctrlholder"><span class="pflow item " id="wfitem_${i}" >
        <g:if test="${isErrorHandler}">
            <span class="info note">on failure:</span>
        </g:if>
        <g:render template="/execution/wfItemView" model="${[item:item,edit:edit,noimgs:noimgs, workflow: workflow, project: project]}"/>
    </span>
        <g:unless test="${stepNum!=null}">
            <g:set var="stepNum" value="${i}"/>
        </g:unless>

    <g:if test="${edit}">
        <span class="wfitemcontrols controls autohide" id="pfctrls_${i}" >
            <g:if test="${!isErrorHandler && !item.errorHandler}">
                <span class="action textbtn wfitem_add_errorhandler">add <g:message code="Workflow.stepErrorHandler.label"/></span>
            </g:if>
            <span class="action" onclick="menus.showRelativeTo(this,'itemdel_${i}',-2,-2);" title="${g.message(code:'Workflow.'+(isErrorHandler?'stepErrorHandler':'step')+'.action.delete.label')}"><g:img file="icon-tiny-removex.png"/></span>
           
            <span class="action textbtn wfitem_edit" >edit</span>
            <g:unless test="${isErrorHandler}">
                <span class="action dragHandle"  title="Drag to reorder"><g:img file="icon-tiny-drag.png"/></span>
            </g:unless>
        </span>
        <div id="itemdel_${i}" class="confirmMessage popout confirmbox"  style="display:none;">
            <g:message code="Workflow.${isErrorHandler ? 'stepErrorHandler' : 'step'}.action.confirmDelete.label" args="${[stepNum+1]}"/>
            <span class="action button small textbtn" onclick="['itemdel_${i}'].each(Element.hide);">No</span>
            <span class="action button small textbtn" onclick="_doRemoveItem('${i}','${stepNum}',${isErrorHandler?true:false});">Yes</span>
        </div>

        <g:javascript>
        fireWhenReady('wfitem_${i}',function(){
            $('wfitem_${i}').select('span.autoedit').each(function(e){
                Event.observe(e,'click',function(evt){
                    var f=$('workflowContent').down('form');
                    if(!f || 0==f.length){
                        _wfiedit("${i}","${stepNum}",${isErrorHandler?true:false});
                    }
                });
            });
            $('pfctrls_${i}').select('span.wfitem_edit').each(function(e){
                Event.observe(e,'click',function(evt){
                    var f=$('workflowContent').down('form');
                    if(!f || 0==f.length){
                        _wfiedit("${i}","${stepNum}",${isErrorHandler?true:false});
                    }
                });
            });

            $('pfctrls_${i}').select('span.wfitem_add_errorhandler').each(function(e){
                Event.observe(e,'click',function(evt){
                    var f=$('workflowContent').down('form');
                    if(!f || 0==f.length){
                        _wfishownewErrorHandler("${i}","${stepNum}");
                    }
                });
            });
            });
        </g:javascript>
    </g:if>
        <div class="clear"></div>
</div>
</div>