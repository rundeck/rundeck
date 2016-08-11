<div id="%{--
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

${enc(attr:key)}_tooltip" style="display:none;" class="detailpopup node_entry ${islocal?'server':''} tooltipcontent node_filter_link_holder"
     data-node-filter-link-id="${enc(attr:nodefilterLinkId?:'')}" >
    <span >
        <i class="rdicon node ${runnable ? 'node-runnable' : ''} icon-small"></i>
        <g:enc>${node.nodename}</g:enc>
    </span>
    <tmpl:nodeFilterLink key="name" value="${node.nodename}"
                         linkicon="glyphicon glyphicon-circle-arrow-right"/>
    <span class="nodedesc"></span>
    <div class="nodedetail">
    <g:render template="/framework/nodeDetailsSimple" bean="${node}" var="node"/>
    </div>
</div>
