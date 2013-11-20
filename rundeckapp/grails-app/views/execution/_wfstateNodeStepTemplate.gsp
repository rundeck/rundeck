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
    <div class="nodectx">
        <span class="textbtn isnode execstate" data-bind="nodename" data-bind-attr="data-execstate:executionState">${node?.encodeAsHTML()}</span >
        <i class="auto-caret"></i>
    </div>
</div>
</g:if>

<div class="col-sm-2 ${!overall?'col-sm-offset-3':''}">
    <span class="${overall?'wfnodecollapse':''}">
    <span class="stepident" data-bind="title:stepctxdesc">
        <i class="rdicon icon-small" data-bind-class="type"></i>
        <span data-bind="stepident"></span>
    </span>
        %{--<span class="stepctx"><span class="subctx" data-bind="substepctx"></span><span--}%
                %{--data-bind="mainstepctx">${ident?.context.collect { it.step }.join("/")}</span>.</span>--}%

    </span>
    <g:if test="${overall}">
        <div class="wfnodecollapse" style="display: none">
            <a class="btn btn-xs btn-default" href="#"
               data-bind-action="nodeoutput"
               data-bind-attr="data-node:nodename">Output <i class="auto-caret"></i></a>
        </div>
    </g:if>
</div>

<div class="col-sm-2">
    <span class="execstate isnode execstatedisplay"
          data-bind="executionState"
          data-bind-attr="data-execstate:executionState"
          data-execstate="${state?.executionState}">${state?.executionState}</span>
</div>


<div class="col-sm-2">
    <span class="${overall ? 'wfnodecollapse collapse in' : ''}">
    <span class="execstart info time" data-bind="startTime" data-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.startTime}"/></span>
    </span>
</div>

<div class="col-sm-3">
    <span class="${overall ? 'wfnodecollapse collapse in' : ''}">
    <span class="execend  info time" data-bind="duration" xdata-bind-format="moment:h:mm:ss a"><g:formatDate date="${state?.endTime}"/></span>
    </span>
</div>
