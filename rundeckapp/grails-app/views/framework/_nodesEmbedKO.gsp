<g:if test="${params.declarenone}">
    <span data-bind="if: !total() || total()<1">
        <span class="warn note"><g:message code="none"/></span>
    </span>
</g:if>
<g:set var="max" value="${-1}"/>
<g:if test="${params.maxShown}">
    <g:set var="max" value="${params.int('maxShown')}"/>
</g:if>
<span data-bind="if: total()>100">
    <a href="#embednodeset" class="textbtn textbtn-default " data-toggle="collapse">
        <span data-bind="messageTemplate: total()"><g:message code="show.all.0.nodes" /></span>
        <i class="auto-caret"></i>
    </a>
</span>
<span id="embednodeset" class=" ansicolor-on matchednodes embed embed_clean"
      data-bind="if: total()<=100">
    <span data-bind="foreach: {data: nodeSet().nodes, 'as': 'node'} ">

        <a tabindex="0"
           role="button"
           class="node_ident node_ident textbtn-default textbtn-plain"
           data-toggle="popover"
           data-placement="bottom"
           data-trigger="focus"
           data-popover-template-class="popover-wide"

           data-bind="css: {server: islocal},
                  css: $root.nodeSet().nodeCss(attributes),
                  style: $root.nodeSet().nodeStyle(attributes),
                  attr: { 'data-node': nodename, 'data-popover-content-ref': '#node_pop_'+$index() },
                  bootstrapPopover: true,
                  bootstrapPopoverContentRef: '#node_pop_'+$index()
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

        <div data-bind="attr: { 'id': 'node_pop_'+$index() }, css: {server: islocal }"
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
                <g:render template="nodeDetailsSimpleKO"/>
            </div>
        </div>

    </span>
</span>
