<div xdata-bind="if: view()=='table'">
    <div class="row">
        <div class="col-sm-12">
            <div class=" col-inset">

                <span id="tagdemo">
                <i class="glyphicon glyphicon-tags text-muted"></i>

                <span data-bind="if: nodeSet().tagsummary">
                    <span data-bind="foreach: nodeSet().tagsummary">
                        <span class="summary nodetags">

                            <node-filter-link params="
                                        filterkey: 'tags',
                                        filterval: tag,
                                        suffix: ' ('+ko.unwrap(value)+')',
                                        linktext: tag,
                                        classnames: 'tag textbtn',
                                        tag: tag,
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
            <table cellpadding="0" cellspacing="0" width="100%" id="nodesTable" class="nodesTable" >
                <tr>
                    <th><g:message code="Node"/></th>
                    <!--ko foreach: filterColumns-->
                    <th data-bind="text: $data"></th>
                    <!--/ko -->

                    <!-- ko if: useDefaultColumns -->
                        <th><g:message code="resource.metadata.entity.tags"/></th>
                        <th colspan="3" class="text-center"><g:message code="user.at.host" /></th>
                    <!-- /ko -->
                    <th></th>
                </tr>
                <tbody data-bind="foreach: {data: nodeSet().nodes, as: 'node' } ">
                <tr class=" node_entry  hover-action-holder ansicolor-on" data-bind="css: {server: islocal}">
                    <td class="nodeident" data-bind="attr: {title: attributes.description}" >

                        <a href="#"
                           data-toggle="collapse"
                           data-bind="attr: {href: '#detail_'+$index() }"
                           class="textbtn textbtn-default "
                        >
                            <i class="auto-caret"></i>
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
                        </a>

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
                    <td class="username"  title="${message(code:"node.metadata.username")}">
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
                        <span data-bind="if: !ko.unwrap(attributes.hostname)">
                            <span class="text-warning" title="${message(code:"node.hostname.unset.description")}"
                            >
                                <g:message code="node.hostname.unset.label" />
                            </span>
                        </span>
                    </td>
                    <!--/ko -->


                    <td>
                    %{--remote edit urls --}%
                    <span data-bind="if: node.attributes['remoteUrl']">
                        <span class="textbtn "
                              title="${message(code:"edit.this.node.via.remote.url")}"
                              data-bind="click: function(){$root.triggerNodeRemoteEdit(node);}"

                        >
                            <g:message code="edit.ellipsis" />
                        </span>
                    </span>
                    <span data-bind="if: node.attributes['editUrl']">
                        <a href="#"
                           target="_blank"
                            data-bind="attr: { href: $root.nodeSet().expandNodeAttributes(node.attributes,node.attributes['editUrl']()) }"
                           title="${message(code:"opens.a.link.to.edit.this.node.at.a.remote.site")}"><g:message code="button.Edit.label" /></a>
                    </span>
                    </td>
                </tr>
                <tr class="detail_content nodedetail collapse collapse-expandable"
                    data-bind="css: { server: islocal }, attr: {id: 'detail_'+$index() }">
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
                <li data-bind="css: {disabled: page()==0 || $root.loading()}">
                    <a
                        href="#"
                            class="btn btn-xs btn-default"
                            data-bind="click: $root.browseNodesPagePrev, visible: $root.maxPages() > 1, attr: {href: $root.browseNodesPagePrevUrl() }"
                            title="${message(code:"Previous")}"><g:message code="default.paginate.prev"/></a>
                </li>

                <!-- ko  foreach: {data: pageNumbersSkipped(), as: 'num'} -->
                        <li data-bind="css: {'active': num==$root.page(), 'disabled': num == '..' || $root.loading() }">
                            <!-- ko if: num == '..' -->
                                <a href="#" > &hellip; </a>
                            <!-- /ko -->
                            <!-- ko if: num != '..' -->
                                <a href="#" data-bind="text: num+1, click: $root.browseNodesPage, attr: {href: $root.browseNodesPageUrl(num)  }"></a>
                            <!-- /ko -->
                        </li>
                <!-- /ko -->

                        <li data-bind="css: {disabled: page()==maxPages() || $root.loading() } ">
                            <a
                                    href="#"
                                    class="btn btn-xs btn-default"
                                    data-bind="click: $root.browseNodesPageNext, visible: $root.maxPages() > 1, attr: {href: $root.browseNodesPageNextUrl() }"
                                    title="${message(code:"Next")}">
                                <g:message code="default.paginate.next"/>
                            </a>
                        </li>

                    </ul>

                </span>
                <div class="form-inline">
                    <div class="form-group form-group-sm">
                        <label>
                            <g:message code="jump.to" />
                            <input class="form-control input-sm" type="number" min="1" data-bind="value: pageDisplay, attr: {max: maxPages}, disable: $root.loading()"/>
                        </label>
                    </div>
                    <div class="form-group form-group-sm">
                        <label>
                            <g:message code="per.page" />
                            <input class="form-control input-sm" type="number" min="1" max="100" data-bind="value: pagingMax, disable: $root.loading()"/>
                        </label>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="col-sm-12">
            <span data-bind="if: loading()"  class="text-info">
                <i class="glyphicon glyphicon-time"></i>
                <g:message code="loading.matched.nodes"/>
            </span>
        </div>
    </div>
</div>

<div id="remoteEditholder" style="display:none" class="popout">
    <span id="remoteEditHeader">
        <span class="welcomeMessage">Edit node: <i class="rdicon node icon-small"></i> <span id="editNodeIdent"></span></span>
    </span>
    <span class="toolbar" id="remoteEditToolbar">
        <span class="action " onclick="_remoteEditCompleted();" title="Close the remote edit box and discard any changes"><g:img file="icon-tiny-removex-gray.png" /> Close remote editing</span>
    </span>
    <div id="remoteEditResultHolder" class="info message" style="display:none">
        <span id="remoteEditResultText" class="info message" >
        </span>
        <span class="action " onclick="_remoteEditContinue();"> Continue&hellip;</span>
    </div>
    <div id="remoteEditError" class="error note" style="display:none">
    </div>
    <div id="remoteEditTarget" >

    </div>
</div>