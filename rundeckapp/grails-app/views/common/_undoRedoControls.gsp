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
<div style="margin-bottom:10px; ">
    <g:if test="${undo}">
        <span class="action button small" onclick="_doUndoAction('${key?.encodeAsJavaScript()}');">Undo</span>
    </g:if>
    <g:else>
        <span class="button disabled small">Undo</span>
    </g:else>
    <g:if test="${redo}">
        <span class="action button small" onclick="_doRedoAction('${key?.encodeAsJavaScript()}');">Redo</span>
    </g:if>
    <g:else>
        <span class="button disabled small">Redo</span>
    </g:else>
    <g:if test="${undo || redo}">
        <span class="action button small" onclick="menus.showRelativeTo(this,'revert_${rkey}');">Revert All Changes</span>

        <div id="revert_${rkey}" class="confirmMessage popout confirmbox" style="display:none">
            Really revert ${revertConfirm?revertConfirm:'all changes'}?
            <span class="action button small textbtn" onclick="['revert_${rkey}'].each(Element.hide);">No</span>
            <span class="action button small textbtn" onclick="_doRevertAction('${key?.encodeAsJavaScript()}');">Yes</span>
        </div>
    </g:if>
</div>