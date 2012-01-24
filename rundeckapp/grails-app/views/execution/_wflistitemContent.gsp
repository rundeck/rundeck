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
        <g:render template="/execution/wfItemView" model="${[item:item,edit:edit,noimgs:noimgs,workflow:workflow,project:project]}"/>
    </span>

    <g:if test="${edit}">
        <span class="wfitemcontrols controls autohide" id="pfctrls_${i}" >
            <span class="action" onclick="menus.showRelativeTo(this,'itemdel_${i}',-2,-2);" title="Delete this ${g.message(code:'Workflow.step.label')}"><g:img file="icon-tiny-removex.png"/></span>
           
            <span class="action textbtn" onclick="_wfiedit(${i});">edit</span>
            <span class="action dragHandle"  title="Drag to reorder"><g:img file="icon-tiny-drag.png"/></span>

        </span>
        <div id="itemdel_${i}" class="confirmMessage popout confirmbox"  style="display:none;">
            Really delete ${g.message(code:'Workflow.step.label')} ${i+1}?
            <span class="action button small textbtn" onclick="['itemdel_${i}'].each(Element.hide);">No</span>
            <span class="action button small textbtn" onclick="_doRemoveItem(${i});">Yes</span>
        </div>

        <g:javascript>
        fireWhenReady('wfitem_${i}',function(){
            $('wfitem_${i}').select('span.autoedit').each(function(e){
                Event.observe(e,'click',function(evt){
                    var f=$('workflowContent').down('form');
                    if(!f || 0==f.length){
                        _wfiedit(${i});
                    }
                });
            });
            });
        </g:javascript>
    </g:if>
        <div class="clear"></div>
</div>
</div>