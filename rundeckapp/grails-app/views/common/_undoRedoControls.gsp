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
    _undoRedoControls.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Aug 4, 2010 4:19:25 PM
    $Id$
 --%>
<g:set var="rkey" value="${g.rkey()}"/>
<div style="margin-bottom:10px; " id="undoredo${rkey}">
    <g:if test="${undo}">
        <span class="btn btn-xs btn-default " onclick="_doUndoAction('${g.enc(js:key)}');">
            <i class="glyphicon glyphicon-step-backward"></i>
            Undo
        </span>
    </g:if>
    <g:else>
        <span class="btn btn-xs btn-default disabled "><i class="glyphicon glyphicon-step-backward"></i> Undo</span>
    </g:else>
    <g:if test="${redo}">
        <span class="btn btn-xs btn-default " onclick="_doRedoAction('${g.enc(js:key)}');">
            Redo
            <i class="glyphicon glyphicon-step-forward"></i>
        </span>
    </g:if>
    <g:else>
        <span class="btn btn-xs btn-default disabled ">Redo <i class="glyphicon glyphicon-step-forward"></i></span>
    </g:else>
    <g:if test="${undo || redo}">
        <span class="btn btn-xs btn-default "
              data-toggle="popover"
              data-popover-content-ref="#revert_${enc(attr:rkey)}"
              data-placement="bottom"
              data-trigger="click"
              id="revertall_${enc(attr:rkey)}"
        >
            <i class="glyphicon glyphicon-fast-backward"></i>
            Revert All Changes</span>

        <div id="revert_${enc(attr:rkey)}" class="confirmMessage popout confirmbox" style="display:none">
            <div class="text-warning">Really revert <g:enc>${revertConfirm?:'all changes'}</g:enc>?</div>

            <span class="btn btn-xs btn-default " onclick="jQuery('#revertall_${rkey}').popover('hide');">No</span>
            <span class="btn btn-xs btn-warning " onclick="jQuery('#revertall_${rkey}').popover('destroy');_doRevertAction('${g.enc(js:key)}');">Yes</span>
        </div>
        <g:javascript>
    _initPopoverContentRef("#undoredo${enc(js:rkey)}");
        </g:javascript>
    </g:if>
</div>
