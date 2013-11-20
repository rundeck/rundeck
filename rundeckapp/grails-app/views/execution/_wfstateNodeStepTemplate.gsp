%{--
  Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  --}%

<g:if test="${overall}">
<div class="col-sm-3 action nodectx" data-bind-action="nodeexpand" data-bind-attr="data-node:nodename">
    <div class="textbtn isnode execstate nodename" data-bind="title:nodename"
         data-bind-attr="data-execstate:executionState">
        <i class="auto-caret"></i>
        <span data-bind="nodename">${node?.encodeAsHTML()}</span>
    </div>
</div>
</g:if>

<div class="col-sm-2 ${!overall?'col-sm-offset-3 action':''}" data-bind-action="stepoutput"
     data-bind-attr="data-node:nodename,data-stepctx:stepctx">
    <g:if test="${overall}">
        <span class="execstate" data-bind-attr="data-execstate:summaryState">
            <span data-bind="summary"></span>
        </span>
    </g:if>
    <g:else>
        <span class="stepident execstate"
              data-bind="title:stepctxdesc"
              data-bind-attr="data-execstate:executionState">
            <i class="auto-caret"></i>
            <i class="rdicon icon-small" data-bind-class="type"></i>
            <span data-bind="html:stepident"></span>
        </span>
    </g:else>
</div>

<div class="col-sm-2">
    <span class="execstate isnode execstatedisplay"
          data-bind="title:executionState"
          data-bind-attr="data-execstate:executionState"
          data-execstate="${state?.executionState}"></span>
</div>


<div class="col-sm-2">
    %{--<g:unless test="${overall}">--}%
    <span class="${overall ? 'wfnodecollapse ' : ''}">
    <span class="execstart info time" data-bind="startTime" data-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.startTime}"/></span>
    </span>
    %{--</g:unless>--}%
</div>

<div class="col-sm-3">
    %{--<g:unless test="${overall}">--}%
    <span class="">
    <span class="execend  info time" data-bind="duration" xdata-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.endTime}"/></span>
    </span>
    %{--</g:unless>--}%
</div>
