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
    _wflistContent.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jul 27, 2010 2:18:34 PM
    $Id$
 --%>
<g:if test="${workflow && workflow?.commands}">
<g:each in="${workflow.commands}" var="item" status="i">
    <li class="${i%2==1?'alternate':''}" data-wfitemnum="${i}">
        <div id="wfli_${i}">
        <g:render template="/execution/wflistitemContent" model="${[i:i,stepNum: i,item:item,workflow:workflow,edit:edit,highlight:highlight,noimgs:noimgs, project: project]}"/>
        </div>
        <g:if test="${item.errorHandler}">
            <ul class="wfhandleritem ${item.errors?.hasFieldErrors('errorHandler') ? 'fieldError' : ''}">

                <li id="wfli_eh_${i}" ><g:render template="/execution/wflistitemContent"
                          model="${[i: 'eh_' + i, stepNum:i, item: item.errorHandler, workflow: workflow, edit: edit, highlight: highlight, noimgs: noimgs, isErrorHandler:true]}"/>
                    <g:hasErrors bean="${item}" field="errorHandler">
                        <span class="info error">
                            <g:eachError field="errorHandler" bean="${item}" var="err">
                                <g:message error="${err}" encodeAs="HTML"/>
                            </g:eachError>
                        </span>
                    </g:hasErrors>
                </li>

            </ul>
        </g:if>
        <g:else>
            <ul class="wfhandleritem" style="display: none" data-wfitemnum="${enc(attr:i)}">

                <li id="wfli_eh_${enc(attr:i)}"></li>

            </ul>
        </g:else>
    </li>
</g:each>
</g:if>
<g:if test="${workflow && workflow.commands && null!=highlight}">
    <g:javascript>
        fireWhenReady('wfivis_${enc(js:highlight)}',function(){
            jQuery("#wfivis_${enc(js:highlight)}").fadeTo("slow",1);
        });
    </g:javascript>
</g:if>
