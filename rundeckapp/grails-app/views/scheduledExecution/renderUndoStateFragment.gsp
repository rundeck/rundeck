<%--
 Copyright 2008-2010 DTO Labs, Inc. (http://dtolabs.com)
  
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 
      http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 
 More information on this software can be found at: http://dtolabs.com
 
 --%>
 <%--
    renderUndoStateFragment.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Jul 27, 2010 6:51:13 PM
    $Id$
 --%>


<div style="margin-bottom:10px; ">
    <g:if test="${undo}">
        <span class="action button small" onclick="_doUndoAction();">Undo</span>
    </g:if>
    <g:else>
        <span class="button disabled small">Undo</span>
    </g:else>
    <g:if test="${redo}">
        <span class="action button small" onclick="_doRedoAction();">Redo</span>
    </g:if>
    <g:else>
        <span class="button disabled small">Redo</span>
    </g:else>
<g:if test="${undo || redo}">
        <span class="action button small" onclick="menus.showRelativeTo(this,'wfchangerevert');">Revert All Changes</span>

        <div id="wfchangerevert" class="confirmMessage popout confirmbox" style="display:none">
            Really revert the Workflow?
            <span class="action button small textbtn" onclick="['wfchangerevert'].each(Element.hide);">No</span>
            <span class="action button small textbtn" onclick="_doResetWFAction();">Yes</span>
        </div>
    </g:if>
</div>
