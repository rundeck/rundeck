<div xdata-bind="if: view()=='table'">
    <div class="row">
        <div class="col-sm-12">
            <div class=" col-inset">

                <span id="tagdemo">
                    %{--<g:if test="${!hidetop}">--}%
                    <i class="glyphicon glyphicon-tags text-muted"></i>


                <span data-bind="if: nodeSet().tagsummary">
                    <span data-bind="foreachprop: nodeSet().tagsummary">
                        <span class="summary nodetags">

                            %{--<g:elseif test="${action}">--}%
                                %{--<span class="${enc(attr:action.classnames)}" onclick="${enc(attr:action.onclick)}"--}%
                                      %{--data-tag="${enc(attr:tag)}" title="Filter by tag: ${enc(attr:tag)}">--}%
                                    %{--<g:enc>${tag}:${tagsummary[tag]}</g:enc>--}%
                                %{--</span>--}%
                            %{--</g:elseif>--}%
                            %{--<g:else>--}%
                                %{--<g:enc>${tag}:${tagsummary[tag]}</g:enc>--}%
                            %{--</g:else>--}%

                            <node-filter-link params="
                                        filterkey: 'tags',
                                        filterval: key,
                                        suffix: ' ('+ko.unwrap(value)+')',
                                        linktext: key,
                                        classnames: 'tag textbtn',
                                        tag: key,
                                        "></node-filter-link>
                        </span>
                    </span>
                    </span>
                </span>
            </div>
        </div>
    </div>
    <div class="row row-space">
        <div class="col-sm-12 ">
            <table cellpadding="0" cellspacing="0" width="100%" id="nodesTable2" class="nodesTable" >
                <tr>
                    <th>Node</th>
                    <!--ko foreach: filterColumns-->
                    <th data-bind="text: $data"></th>
                    <!--/ko -->

                    <!-- ko if: useDefaultColumns -->
                        <th>Tags</th>
                        <th colspan="3" class="text-center">User @ Host</th>
                    <!-- /ko -->
                    <th></th>
                </tr>
                <tbody data-bind="foreach: {data: nodeSet().nodes, as: 'node' } ">
                <tr class=" node_entry  hover-action-holder ansicolor-on" data-bind="css: {server: islocal}">
                    <td class="nodeident" data-bind="attr: {title: attributes.description}" >

                        <span data-bind="click: node.toggleExpanded, css: { expanded: node.expanded(), closed: !node.expanded() } "
                              class="textbtn  expandComponentControl toggle">
                            <i class="glyphicon " data-bind="css: { 'glyphicon-chevron-down': node.expanded(), 'glyphicon-chevron-right': !node.expanded() }"></i>
                            <span class="node_ident" data-bind="css: {server: islocal}, css: $root.nodeSet().nodeCss(attributes), style: $root.nodeSet().nodeStyle(attributes)">
                                    <span data-bind="css: $root.nodeSet().iconCss(attributes), style: $root.nodeSet().iconStyle(attributes)">
                                    <!-- ko if: attributes['ui:icon:name'] -->
                                    <!-- ko with: attributes['ui:icon:name']() -->
                                        <i  data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                                    <!-- /ko -->
                                    <!-- /ko -->
                                    <!-- ko if: !attributes['ui:icon:name'] -->
                                    <i class="rdicon node icon-small" data-bind="css: {authrun: 'node-runnable'}"></i>
                                    <!-- /ko -->
                                    </span>
                                    <span data-bind="text: nodename"></span>
                            </span>
                        </span>

                        <node-filter-link params="
                                        filterkey: 'name',
                                        filterval: nodename,
                                        linkicon: 'glyphicon glyphicon-circle-arrow-right',
                                        "></node-filter-link>

                        <span class="nodedesc"></span>
                        <span class="text-muted ">
                            <!-- ko if: attributes['ui:badges'] -->
                            <!-- ko foreach:   $root.nodeSet().glyphiconBadges(attributes)-->
                            <i  data-bind="css: $root.nodeSet().glyphiconCss($data)"></i>
                            <!-- /ko -->
                            <!-- /ko -->
                            <span data-bind="text: attributes.description"></span>
                        </span>
                    </td>

                    <!--ko foreach: $root.filterColumns -->
                    <td>
                        <span class="value" data-bind="if: $parent.attributes[$data]">
                            <span data-bind="if: $data=='tags'">
                                <span class="nodetags">
                                    <i class="" data-bind="css: {'glyphicon glyphicon-tags text-muted': $parent.tags().size()>0}"></i>
                                    <span data-bind="foreach: $parent.tags">

                                        <node-filter-link params="
                                                    filterkey: 'tags',
                                                    filterval: $data,
                                                    tag: $data
                                                    "></node-filter-link>
                                    </span>
                                </span>
                            </span>
                            <span data-bind="if: $data!='tags'">
                                <span data-bind="text: $parent.attributes[$data]()"></span>
                                <node-filter-link params="
                                                    filterkey: $data,
                                                    filterval: $parent.attributes[$data],
                                                    linkicon: 'glyphicon glyphicon-search textbtn-saturated hover-action',
                                                    classnames: 'textbtn textbtn-info'
                                                    "></node-filter-link>

                            </span>
                        </span>
                    </td>
                    <!--/ko -->
                    <!--ko if: $root.useDefaultColumns -->

                    <td  title="Tags" class="nodetags" >
                        <span data-bind="if: tags">
                            <span class="nodetags">
                                <i class="" data-bind="css: {'glyphicon glyphicon-tags text-muted': tags().size()>0}"></i>
                                <span data-bind="foreach: tags">
                                    <node-filter-link params="
                                                    filterkey: 'tags',
                                                    filterval: $data,
                                                    tag: $data
                                                    "></node-filter-link>
                                </span>
                            </span>
                        </span>
                    </td>
                    <td class="username"  title="Username">
                        <span data-bind="if: attributes.username">


                            <node-filter-link params="
                                        filterkey: 'username',
                                        filterval: attributes.username,
                                        "></node-filter-link>

                            <span class="atsign">@</span>
                        </span>
                    </td>
                    <td class="hostname"  title="Hostname">
                        <span data-bind="if: attributes.hostname">

                            <node-filter-link params="
                                        filterkey: 'hostname',
                                        filterval: attributes.hostname,
                                        "></node-filter-link>


                        </span>
                    </td>
                    <!--/ko -->


                    <td>
                    %{--remote edit urls --}%
                    <span data-bind="if: node.attributes['remoteUrl']">
                        <span class="textbtn "
                              title="Edit this node via remote URL..."
                              data-bind="click: triggerNodeRemoteEdit"

                        >
                            Edit&hellip;
                        </span>
                    </span>
                    <span data-bind="if: node.attributes['editUrl']">
                        <a href="#"
                           target="_blank"
                            data-bind="attr: { href: $root.nodeSet().expandNodeAttributes(node.attributes,node.attributes['editUrl']()) }"
                           title="Opens a link to edit this node at a remote site.">Edit</a>
                    </span>
                    </td>
                </tr>
                <tr class="detail_content nodedetail "  data-bind="css: { server: islocal },  visible: node.expanded()  ">
                    <td colspan="4" data-bind="attr: { colspan: $root.totalColumnsCount }">

                        <g:render template="nodeDetailsSimpleKO" model="[useNamespace:true]"/>


                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
    %{--paging links--}%
    <div data-bind="if: hasPaging">
        <div class="row row-space" data-bind="visible: hasPaging">
            <div class="col-sm-12" id="nodesPaging">
                <span class="paginate">
                <ul class="pagination pagination-sm pagination-embed">
                <li data-bind="css: {disabled: page()==0}">
                    <a
                        href="#"
                            class="btn btn-xs btn-default"
                            data-bind="click: $root.nodesPagePrev, visible: $root.maxPages() > 1"
                            title="Previous"><g:message code="default.paginate.prev"/></a>
                </li>

                <!-- ko  foreach: {data: pageNumbersSkipped(), as: 'num'} -->
                        <li data-bind="css: {'active': num==$root.page(), 'disabled': num == '..' }">
                            <!-- ko if: num == '..' -->
                                <a href="#" > &hellip; </a>
                            <!-- /ko -->
                            <!-- ko if: num != '..' -->
                                <a href="#" data-bind="text: num+1, click: $root.setNodesPage"></a>
                            <!-- /ko -->
                        </li>
                <!-- /ko -->

                        <li data-bind="css: {disabled: page()==maxPages() } ">
                            <a
                                    href="#"
                                    class="btn btn-xs btn-default"
                                    data-bind="click: $root.nodesPageNext, visible: $root.maxPages() > 1"
                                    title="Next">
                                <g:message code="default.paginate.next"/>
                            </a>
                        </li>

                    </ul>

                </span>
                <div class="form-inline">
                    <label class="form-input input-sm">Per page: <input type="number" data-bind="value: pagingMax"/></label>
                </div>
                <span data-bind="if: loading()"  class="text-info">
                    <i class="glyphicon glyphicon-time"></i>
                    <g:message code="loading.matched.nodes"/>
                </span>
            </div>
        </div>
    </div>
</div>