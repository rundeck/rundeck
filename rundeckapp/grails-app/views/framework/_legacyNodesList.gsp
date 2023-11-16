<div class="title">
    <span class="text-h3"><i class="fas fa-sitemap"></i> ${g.message(code:"gui.menu.Nodes")}</span>
</div>
<div class="row" style="margin-bottom: 20px;">
    <div class="col-xs-12 subtitle-head vue-ui-socket">
        <ui-socket section="nodes-page" location="node-filter-input"
                   socket-data="${enc(attr: [filter: filtvalue?:'', showInputTitle: true, autofocus: true].encodeAsJSON())}">

        </ui-socket>
    </div>
</div>
<div id="nodesContent">

    <g:render template="/common/messages"/>

    <div class="container-fluid">
        <div class="row">
            <div class="col-xs-12">
                <div class="card">
                    <div class="card-content vue-tabs">
                        <!-- NAV TABS -->
                        <div class="nav-tabs-navigation">
                            <div class="nav-tabs-wrapper" >
                                <ul class="nav nav-tabs">
                                    <li class="active" id="tab_link_summary">
                                        <a href="#summary" data-toggle="tab">
                                            <g:message code="browse" />
                                        </a>
                                    </li>
                                    <li id="tab_link_result" data-bind="visible: allcount()>=0">
                                        <a href="#result" data-toggle="tab" data-bind="visible: filterIsSet() ">
                                            <g:message code="result" />
                                            <span data-bind="visible: allcount()>=0">
                                                <span data-bind="text: allcount" class="text-info">${total}</span>
                                                <span data-bind="text: nodesTitle()">Node${1 != total ? 's' : ''}</span>
                                            </span>
                                            <span data-bind="visible: allcount()<0" class="text-strong">&hellip;</span>

                                        </a>
                                        <a href="#" data-bind="visible: !filterIsSet() ">
                                            <g:message code="enter.a.filter" />
                                        </a>
                                    </li>

                                </ul>
                            </div>
                            <span data-bind="visible: allcount()>=0" class="pull-right">
                                <span class="tabs-sibling tabs-sibling-compact">
                                    <div class="btn-group pull-right ">
                                        <button class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                                            <g:message code="actions" /> <span class="caret"></span>
                                        </button>
                                        <ul class="dropdown-menu" role="menu">


                                            <g:if test="${g.executionMode(is:'active',project:params.project)}">

                                                <li data-bind="visible: hasNodes()" class="${run_authorized?'':'disabled'}">
                                                    <a href="#" data-bind="${run_authorized?'click: runCommand':''}"
                                                       title="${run_authorized ? '' : message(code:"not.authorized")}"
                                                       class="${run_authorized ? '' : 'has_tooltip'}"
                                                       data-placement="left"
                                                    >
                                                        <i class="glyphicon glyphicon-play"></i>
                                                        <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="run.a.command.on.count.nodes.ellipsis" /></span>
                                                    </a>
                                                </li>
                                            </g:if>
                                            <g:else>

                                                <li data-bind="visible: hasNodes()" class="disabled">
                                                    <a href="#"
                                                       title="${g.message(code:'disabled.execution.run')}"
                                                       class="has_tooltip"
                                                       data-placement="left"
                                                    >
                                                        <i class="glyphicon glyphicon-play"></i>
                                                        <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="run.a.command.on.count.nodes.ellipsis" /></span>
                                                    </a>
                                                </li>
                                            </g:else>

                                            <li class="${job_create_authorized?'':'disabled'}">
                                                <a href="#" data-bind="${job_create_authorized?'click: saveJob':''}"
                                                   title="${job_create_authorized?'':message(code:"not.authorized")}"
                                                   class="${job_create_authorized?'':'has_tooltip'}"
                                                   data-placement="left"
                                                >
                                                    <i class="glyphicon glyphicon-plus"></i>
                                                    <span data-bind="messageTemplate: [total,nodesTitle]"><g:message code="create.a.job.for.count.nodes.ellipsis" /></span>
                                                </a>
                                            </li>
                                        </ul>
                                    </div>
                                </span>
                            </span>
                        </div>



                        <!-- TABS -->
                        <div class="tab-content">
                            <div class="tab-pane " id="result">
                                <!-- ko if: error() -->
                                <div class="row row-space">
                                    <div class="col-sm-12">
                                        <span class="text-danger">
                                            <i class="glyphicon glyphicon-warning-sign"></i>
                                            <span data-bind="text: error()"></span>
                                        </span>
                                    </div>
                                </div>
                                <!-- /ko -->
                                <div class="clear matchednodes" id="nodeview">
                                    <g:render template="allnodesKO" />
                                </div>
                            </div>
                            <div class="tab-pane active" id="summary">

                                <div class="row">

                                    <div class="col-xs-6">
                                        <h5 class="column-title text-uppercase text-strong">
                                            <g:message code="resource.metadata.entity.tags" />
                                        </h5>
                                        <ul data-bind="foreach: nodeSummary().tags" class="list-unstyled">
                                            <li style="display:inline;">
                                                <node-filter-link  params="
                                    classnames: 'label label-muted',
                                    filterkey: 'tags',
                                    filterval: tag,
                                    tag: tag,
                                    count: count
                                ">
                                                </node-filter-link>
                                            </li>
                                        </ul>
                                        <div data-bind="visible: !nodeSummary().tags">
                                            <g:message code="none" />
                                        </div>
                                    </div>

                                    <div class="col-xs-6">

                                        <h5 class="column-title text-uppercase text-strong">
                                            <g:message code="filters" />
                                        </h5>

                                        <ul class="list-unstyled">
                                            <li>
                                                <a href="#" class="nodefilterlink btn btn-default btn-xs" data-node-filter=".*" data-node-filter-all="true">
                                                    <g:message code="all.nodes" />
                                                    <span data-bind="text: nodeSummary().totalCount">0</span>
                                                </a>
                                                <div class="btn-group">
                                                    <button type="button"
                                                            class="btn btn-default btn-xs btn-simple dropdown-toggle"
                                                            style="padding: 0 5px;"
                                                            title="Filter Actions"
                                                            data-toggle="dropdown"
                                                            aria-expanded="false">
                                                        <span class="caret"></span>
                                                    </button>
                                                    <ul class="dropdown-menu" role="menu">
                                                        <li data-bind="visible: '.*'!=$root.nodeSummary().defaultFilter()">
                                                            <a href="#"
                                                               data-bind="click: $root.nodeSummary().setDefaultAll">

                                                                <i class="glyphicon glyphicon-filter"></i>
                                                                <g:message code="set.all.nodes.as.default.filter" />
                                                            </a>
                                                        </li>
                                                        <li data-bind="visible: '.*'==$root.nodeSummary().defaultFilter()">
                                                            <a href="#"
                                                               data-bind="click: $root.nodeSummary().removeDefault">
                                                                <i class="glyphicon glyphicon-ban-circle"></i>
                                                                <g:message code="remove.all.nodes.as.default.filter" />
                                                            </a>
                                                        </li>
                                                    </ul>
                                                </div>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
