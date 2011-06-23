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
    _optlistitemContent.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 2, 2010 4:11:05 PM
    $Id$
 --%>
<div id="optvis_${option.name.encodeAsHTML()}" >
    <div class="optitem optctrlholder">
        <span class="opt item " id="opt_${option.name.encodeAsHTML()}" >
            <g:render template="/scheduledExecution/optView" model="${[option:option,edit:edit]}"/>
        </span>

    <g:if test="${edit}">
        <span class="optctrl opteditcontrols controls autohide" id="optctrls_${option.name.encodeAsHTML()}">
            <span class="action" onclick="menus.showRelativeTo(this,'optdel_${option.name.encodeAsJavaScript()}',-2,-2);" title="Delete this option"><g:img file="icon-tiny-removex.png"/></span>
            <span class="action textbtn" onclick="_optedit('${option.name.encodeAsJavaScript()}',$(this).up('li.optEntry'));" title="Edit this option">edit</span>
        </span>

        <div id="optdel_${option.name.encodeAsHTML()}" class="confirmMessage popout confirmbox"  style="display:none">
            Really delete option ${option.name.encodeAsHTML()}?
            <span class="action button small textbtn" onclick="['optdel_${option.name.encodeAsJavaScript()}'].each(Element.hide);">No</span>
            <span class="action button small textbtn" onclick="_doRemoveOption('${option.name.encodeAsJavaScript()}',$(this).up('li.optEntry'));">Yes</span>
        </div>
        <g:javascript>
        fireWhenReady('opt_${option.name.encodeAsJavaScript()}',function(){
            $('opt_${option.name.encodeAsJavaScript()}').select('span.autoedit').each(function(e){
                Event.observe(e,'click',function(evt){
                    var f=$('optionsContent').down('form');
                    if(!f || 0==f.length){
                        _optedit('${option.name.encodeAsJavaScript()}',$(e).up('li.optEntry'));
                    }
                });
            });
            });
        </g:javascript>
    </g:if>
        <div class="clear"></div>
</div>
</div>