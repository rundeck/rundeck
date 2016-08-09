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

%{--random string for uniqueness--}%
<g:set var="xkey" value="${g.rkey()}"/>
<span  class=" ansicolor-on matchednodes embed embed_clean" data-bind="">
    <span data-bind="foreach: {data: nodeSet().nodes, 'as': 'node'} ">

        <a tabindex="0"
           role="button"
           class="node_ident textbtn-default textbtn-plain"
           data-toggle="popover"
           data-placement="bottom"
           data-trigger="focus"
           data-delay="{&quot;show&quot;:0,&quot;hide&quot;:200}"
           data-popover-template-class="popover-wide"

           data-bind="css: {server: islocal},
                  css: $root.nodeSet().nodeCss(attributes),
                  style: $root.nodeSet().nodeStyle(attributes),
                  attr: { 'data-node': nodename },
                  bootstrapPopover: true,
                  bootstrapPopoverContentRef: '#node_pop_${xkey}_'+$index()
                  ">
            <span data-bind="css: $root.nodeSet().iconCss(attributes), style: $root.nodeSet().iconStyle(attributes)">
                <!-- ko if: attributes['ui:icon:name'] -->
                <!-- ko with: attributes['ui:icon:name']() -->
                <i data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                <!-- /ko -->
                <!-- /ko -->
                <!-- ko if: !attributes['ui:icon:name'] -->
                <i class="rdicon node icon-small" data-bind="css: {authrun: 'node-runnable'}"></i>
                <!-- /ko -->
            </span>
            <span data-bind="text: nodename"></span>
        </a>

        <div data-bind="attr: { 'id': 'node_pop_${xkey}_'+$index() }, css: {server: islocal }"
             style="display:none;"
             class="detailpopup node_entry tooltipcontent node_filter_link_holder"
             data-node-filter-link-id="${enc(attr: nodefilterLinkId ?: '')}">

            <span>
                <i class="rdicon node icon-small" data-bind="css: {authrun: 'node-runnable'}"></i>
                <span data-bind="text: nodename"></span>
            </span>

            <node-filter-link params="
                filterkey: 'name',
                filterval: nodename,
                linkicon: 'glyphicon glyphicon-circle-arrow-right'
                "></node-filter-link>

            <span class="nodedesc"></span>

            <div class="nodedetail">
                <g:render template="/framework/nodeDetailsSimpleKO"/>
            </div>
        </div>

    </span>
</span>

<g:if test="${showLoading}">
    <div data-bind="if: loading() " class="text-info">
        <i class="glyphicon glyphicon-time"></i>
        <g:message code="loading.matched.nodes" />
    </div>
</g:if>
<g:if test="${showTruncated}">
    <div data-bind="if: total() > maxShown()">
        <span data-bind="messageTemplate: [maxShown(), total()]" class="text-info"><g:message code="results.truncated.count.results.shown" /></span>
    </div>
</g:if>

