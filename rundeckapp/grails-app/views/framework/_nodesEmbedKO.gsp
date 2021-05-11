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
<div  class="nodes-embed ansicolor-on matchednodes embed embed_clean" data-bind="">

    <div data-bind="foreach: {data: nodeSet().nodes, 'as': 'node'} " class="row">

        <a
           class=" col-xs-6 node_ident embedded_node tight"
           tabindex="0"
           role="button"
           data-viewport="#section-content"
           data-placement="auto"
           data-container="body"
           data-delay="{&quot;show&quot;:0,&quot;hide&quot;:200}"
           data-popover-template-class="popover-wide"
           data-bind="
                  css: $root.nodeSet().nodeCss(attributes),
                  css2: {
                      server: islocal,
                      'col-lg-2': $root.nodeSet().nodes().length>20,
                      'col-md-3': $root.nodeSet().nodes().length>12,
                      'col-sm-4': $root.nodeSet().nodes().length>8 && $root.nodeSet().nodes().length<13,
                  },
                  style: $root.nodeSet().nodeStyle(attributes),
                  attr: { 'data-node': nodename, title: nodename},
                  bootstrapPopover: true,
                  bootstrapPopoverContentRef: '#node_pop_${xkey}_'+$index(),
                  bootstrapPopoverOptions:{trigger:'click'},
                  bootstrapTooltip: nodename,
                  bootstrapPopoverHideTooltip: true
                  ">
            <span data-bind="css: $root.nodeSet().iconCss(attributes), style: $root.nodeSet().iconStyle(attributes)">
                <!-- ko if: attributes['ui:icon:name'] -->
                <!-- ko with: attributes['ui:icon:name']() -->
                <i data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                <!-- /ko -->
                <!-- /ko -->
                <!-- ko if: !attributes['ui:icon:name'] -->
                <i class="fas fa-hdd"></i>
                <!-- /ko -->
            </span>

            <span data-bind="text: nodename, css: {'node_unselected':unselected}"></span>

            <span data-bind="css: $root.nodeSet().statusIconCss(attributes), style: $root.nodeSet().statusIconStyle(attributes), attr: {title: attributes['ui:status:text']}">
                <!-- ko if: attributes['ui:status:icon'] -->
                <!-- ko with: attributes['ui:status:icon']() -->
                <i  data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                <!-- /ko -->
                <!-- /ko -->
            </span>
        </a>


        <div data-bind="attr: { 'id': 'node_pop_${xkey}_'+$index() }, css: {server: islocal }"
             style="display:none;">
            <div class="_mousedown_popup_allowed detailpopup node_entry tooltipcontent node_filter_link_holder"
                 data-node-filter-link-id="${enc(attr: nodefilterLinkId ?: '')}">
                <span>
                    <i class="fas fa-hdd"></i>
                    <span data-bind="text: nodename"></span>
                </span>

                <node-filter-link params="
                    filterkey: 'name',
                    filterval: nodename,
                    linkicon: 'glyphicon glyphicon-circle-arrow-right'
                    "></node-filter-link>

                <span class="nodedesc"></span>

                <div class="nodedetail" style="overflow-x: scroll;">
                    <g:render template="/framework/nodeDetailsSimpleKO" model="[useNamespace: true, crefText:'$CREF$']"/>
                </div>
            </div>
        </div>

    </div>
</div>

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
