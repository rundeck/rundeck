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
   _optView.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Aug 2, 2010 4:08:55 PM
   $Id$
--%>
<g:set var="rkey" value="${g.rkey()}"/>

<span id="opt_${rkey}" class="optview">
    <span class="optdetail ${edit?'autohilite autoedit':''}" ${edit?'title="Click to edit"':''} ${edit?'':''}>
        <span class=" ${option?.required ? 'required' : ''}" title="${enc(attr:option?.description)}${option?.required ? ' (Required)' : ''}"><g:enc>${option.name}</g:enc></span>
        <span class="">
            <g:truncate max="20" showtitle="true"><g:enc>${option.secureInput && option.defaultValue?'****':option.defaultValue}</g:enc></g:truncate><g:enc>${option.multivalued?'(+)':''}</g:enc>
            <g:if test="${option.secureInput && option.defaultStoragePath}">
                <g:icon name="lock"/>
            </g:if>
        </span>
        <span class="desc"><g:strip>${option.description}</g:strip></span>
    </span>
    <g:if test="${option?.values || option.valuesList}">
        <g:set var="opts" value="${option.values?option.values.sort():option.valuesList.split(',').sort()}"/>
        <div class="popout detailpopup" id="vls_${rkey}_tooltip" style="width:200px;display:none;" >
            <div class="info note">Allowed Values</div>
            <g:each var="val" in="${opts}" status="i"><g:enc>${0!=i?', ':''}</g:enc><span class="valueItem"><g:enc>${val}</g:enc></span></g:each>
        </div>
        <span class="valuesSet">
        <span class="valueslist" id="xvls_${rkey}"
              data-toggle="popover"
              data-trigger="hover"
              data-placement="bottom"
              data-popover-content-ref="#vls_${rkey}_tooltip"><g:enc>${opts ? opts.size() :0}</g:enc> Value${1==opts?.size()?'':'s'}</span>
        </span>
    </g:if>
    <g:elseif test="${option.realValuesUrl}">
        <span class="valuesSet">
        <span class="valuesUrl" title="Values loaded from Remote URL: ${enc(attr:option.realValuesUrl)}">URL</span>
        </span>
    </g:elseif>

    <g:if test="${option.enforced}">
        <span class="enforceSet">
        <span class="enforced" title="Input must be one of the allowed values">Strict</span>
        </span>
    </g:if>
    <g:elseif test="${option.regex}">
        <div class="popout detailpopup" style="display:none; width: 200px" id="rgx_${rkey}_tooltip">
            <div class="info note">Values must match the regular expression:</div>
            <code><g:enc>${option.regex}</g:enc></code>
        </div>
        <span class="enforceSet">
        <span class="regex"
              data-toggle="popover"
              data-trigger="hover"
              data-placement="bottom"
              data-popover-content-ref="#rgx_${rkey}_tooltip"
              id="rgx_${rkey}"><g:enc>${option.regex}</g:enc></span>
        </span>
    </g:elseif>
    <g:else>
        <span class="enforceSet">
        <span class="any" title="No restrictions on input value">None</span>
        </span>
    </g:else>
</span>

<g:javascript>
    fireWhenReady('opt_${enc(js:rkey)}',function(){
        _initPopoverContentRef('#opt_${enc(js: rkey)}');
    });
</g:javascript>

