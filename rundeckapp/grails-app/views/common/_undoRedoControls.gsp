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
    _undoRedoControls.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 4, 2010 4:19:25 PM
    $Id$
 --%>
<g:set var="rkey" value="${g.rkey()}"/>
<div style="margin-bottom:10px; " id="undoredo${rkey}">
    <g:if test="${undo}">
        <span class="btn btn-xs btn-default act_undo"  data-undo-key="${enc(attr:key)}">
            <i class="glyphicon glyphicon-step-backward"></i>
            Undo
        </span>
    </g:if>
    <g:else>
        <span class="btn btn-xs btn-default disabled "><i class="glyphicon glyphicon-step-backward"></i> Undo</span>
    </g:else>
    <g:if test="${redo}">
        <span class="btn btn-xs btn-default act_redo" data-undo-key="${enc(attr: key)}">
            Redo
            <i class="glyphicon glyphicon-step-forward"></i>
        </span>
    </g:if>
    <g:else>
        <span class="btn btn-xs btn-default disabled ">Redo <i class="glyphicon glyphicon-step-forward"></i></span>
    </g:else>
    <g:jsonToken id="reqtoken_undo_${key}" url="${request.forwardURI}"/>
    <g:if test="${undo || redo}">
        %{--popover trigger is initialized on click, defined in jquery init from scheduledExecution/_edit.gsp --}%
        <span class="btn btn-xs btn-default act_revert_popover"
              data-toggle="popover"
              data-popover-content-ref="#revert_${enc(attr:rkey)}"
              data-placement="bottom"
              data-trigger="click"
              data-popover-key="${enc(attr:rkey)}"
              id="revertall_${enc(attr:rkey)}"
        >
            <i class="glyphicon glyphicon-fast-backward"></i>
            Revert All Changes</span>

        <div id="revert_${enc(attr:rkey)}" class="confirmMessage popout confirmbox" style="display:none">
            <div class="text-warning">Really revert <g:enc>${revertConfirm?:'all changes'}</g:enc>?</div>

            <span class="btn btn-xs btn-default act_revert_cancel" data-popover-key="${enc(attr: rkey)}" data-undo-key="${enc(attr: key)}">No</span>
            <span class="btn btn-xs btn-warning act_revert_confirm" data-popover-key="${enc(attr: rkey)}" data-undo-key="${enc(attr: key)}">Yes</span>
        </div>
    </g:if>
</div>
