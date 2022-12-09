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
<%@ page import="rundeck.User;com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants" %>


<g:uploadForm controller="scheduledExecution" method="post" useToken="true"
              params="[project: scheduledExecution.project]" role="form" >
<input type="hidden" name="id" value="${enc(attr:scheduledExecution?.extid)}"/>
<div id="exec_options_form">
    <!-- BEGIN: firefox hack https://bugzilla.mozilla.org/show_bug.cgi?id=1119063 -->
    <input type="text" style="display:none" class="ixnay">
    <input type="password" style="display:none" class="ixnay">
    <g:javascript>
    jQuery(function(){
        var nay=function(){jQuery('.ixnay').val('');},ix=setTimeout;
        nay(); ix(nay,50); ix(nay,200); ix(nay, 1000);
    });
    </g:javascript>
    <!-- END: firefox hack -->
<div class="exec-options-body container-fluid">



<g:if test="${!hideHead}">
    <div class="row exec-options-header">
        <div class="col-sm-12">
            <tmpl:showHead scheduledExecution="${scheduledExecution}" isScheduled="${isScheduled}" iconName="icon-job"
                           runPage="true" jobDescriptionMode="collapsed"/>
        </div>
    </div>

</g:if>
    <g:set var="project" value="${scheduledExecution?.project ?: params.project?:request.project?: projects?.size() == 1 ? projects[0].name : ''}"/>
    <g:embedJSON id="filterParamsJSON" data="${[filterName: params.filterName, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
    <div class=" collapse" id="queryFilterHelp">
        <div class="help-block">
            <g:render template="/common/nodefilterStringHelp"/>
        </div>
    </div>



    <g:if test="${hideHead}">
        <section>
            <div class="row">
                <g:render template="/scheduledExecution/execOptionsFormButtons"
                          model="[scheduledExecution: scheduledExecution, hideCancel: hideCancel, showRunLater: true]"/>
            </div>
        </section>
    </g:if>
    <g:if test="${params.meta instanceof Map}">
        <g:each in="${params.meta}" var="metaprop">
            <g:hiddenField name="meta.${metaprop.key}" value="${metaprop.value}"/>
        </g:each>
    </g:if>

    <g:if test="${scheduledExecution?.options}">
    <section class="form-horizontal section-pad-top-lg ${hideHead ? 'section-separator' : ''}">
        <g:render template="editOptions"
                  model="${[scheduledExecution: scheduledExecution, selectedoptsmap: selectedoptsmap, selectedargstring: selectedargstring, authorized: authorized, jobexecOptionErrors: jobexecOptionErrors, optiondependencies: optiondependencies, dependentoptions: dependentoptions, optionordering: optionordering]}"/>
    </section>
    </g:if>
    <g:elseif test="${!scheduledExecution?.options }">
        <g:render template="/common/messages"/>
    </g:elseif>

    <g:render template="jobComponentProperties"
              model="[
                      jobComponents:jobComponents,
                      sectionName:'nodes',
                      jobComponentValues:jobComponentValues
              ]"
    />

    <section class="form-horizontal section-separator">
        <div class="vue-ui-socket">
            <div>
                <ui-socket section="resources-override-filter" location="top" :event-bus="eventBus" />
            </div>
        </div>
    </section>

    <section class="form-horizontal section-separator"
             style="${wdgt.styleVisible(if: nodesetvariables && !failedNodes || nodesetempty || nodes)}">

        <div class="form-group">
            <label class="col-sm-2 control-label">
                <g:message code="Node.plural"/>
            </label>


        <div class="col-sm-10">
        <g:if test="${nodesetvariables && !failedNodes}">
            %{--show node filters--}%
            <div>
                <span class="query form-control-static ">
                   <code class="queryvalue text"><g:enc>${nodefilter}</g:enc></code>
                </span>
            </div>

            <p class="form-control-static text-info">
                <g:message code="scheduledExecution.nodeset.variable.warning"/>
            </p>
        </g:if>
        <g:elseif test="${nodesetempty }">
            <div class="alert alert-warning">
                <g:message code="scheduledExecution.nodeset.empty.warning"/>
            </div>
        </g:elseif>


            <g:set var="selectedNodes"
                   value="${failedNodes? failedNodes.split(',').findAll{it}:selectedNodes instanceof String? selectedNodes.split(',').findAll{it}:selectedNodes instanceof Collection? selectedNodes:null}"/>
            <g:embedJSON id="selectedNodesJson" data="${selectedNodes?:[]}"/>
            <g:embedJSON id="allNodesJson" data="${nodes?nodes*.nodename:[]}"/>
            <g:embedJSON id="namegroupsJson" data="${namegroups?:[:]}"/>
            <g:embedJSON id="namegrouptagsJson" data="${grouptags?:[:]}"/>

            <div class="row" data-ko-bind="runformoptions">
                <div class="col-sm-12 ">

                    <div class="checkbox checkbox-inline" data-bind="visible: canOverrideFilter() || !hasDynamicFilter()">
                        <input name="extra._replaceNodeFilters"
                               value="true"
                               type="checkbox"
                               data-bind="checked: changeTargetNodes"
                               id="doReplaceFilters"/>
                        <label for="doReplaceFilters">
                            <g:message code="change.the.target.nodes"/>
                            <!-- ko if: !isNodeFilterVisible() -->
                            (<span data-bind="text: selectedNodes().length, css: {'text-info':selectedNodes().length,'text-danger':!selectedNodes().length}" ></span>)
                            <!-- /ko -->
                            <!-- ko if: isNodeFilterVisible() -->
                            (<span data-bind="
                                visible: isNodeFilterVisible() && nodeFilter, text: nodeFilter.total,
                                css: {'text-info':nodeFilter.total,'text-danger':!nodeFilter.total()}" >0</span>)
                            <!-- /ko -->
                        </label>

                    </div>

                        <div class="radio radio-inline" data-bind="visible: changeTargetNodes() && canOverrideFilter() && !hasDynamicFilter()">
                            <input id="cherrypickradio"
                                   type="radio"
                                   name="extra.nodeoverride"
                                   checked="checked"
                                   value="cherrypick"
                                   data-bind="checked: nodeOverride"/>
                            <label for="cherrypickradio">
                                <g:message code="select.nodes" />
                            </label>
                        </div>

                        <div class="radio radio-inline" data-bind="visible: changeTargetNodes() && canOverrideFilter() && !hasDynamicFilter()">
                            <input id="filterradio"
                                   type="radio"
                                   name="extra.nodeoverride"
                                   value="filter"
                                   data-bind="checked: nodeOverride"/>
                            <label for="filterradio">
                                <g:message code="job.run.override.node"/>
                            </label>
                        </div>


                </div>

            </div>
            <div>
                <div class=" matchednodes embed jobmatchednodes group_section " id="nodeSelect">
                <%--
                 split node names into groups, in several patterns
                  .*\D(\d+)
                  (\d+)\D.*
                --%>
                    <div data-ko-bind="runformoptions" data-bind="visible: isCherrypickVisible">

                <g:if test="${!nodesetvariables && nodes}">
                    <div class=" ">
                            Select Nodes
                            <!-- ko if: isCherrypickVisible -->
                            (<span data-bind="text: selectedNodes().length, css: {'text-info':selectedNodes().length,'text-danger':!selectedNodes().length}" ></span>)
                            <!-- /ko -->

                        <!-- ko if: isCherrypickVisible -->
                            <span class="btn btn-xs btn-simple  btn-hover selectall" data-bind="click: selectAllNodes">
                                <g:icon name="check"/>
                                <g:message code="select.all" />
                            </span>
                            <span class="btn btn-xs btn-simple  btn-hover selectnone"  data-bind="click: selectNoNodes">
                                <g:icon name="unchecked"/>
                                <g:message code="select.none" />
                            </span>
                            <!-- /ko -->
                            <g:if test="${tagsummary}">
                                <g:render template="/framework/tagsummary"
                                          model="${[tagsummary:tagsummary,action:[classnames:'label label-muted autoclickable obs_tag_group',onclick:'']]}"/>
                            </g:if>
                    </div>
                <g:if test="${namegroups}">
                    <div>
                    <g:each in="${namegroups.keySet().sort()}" var="group">
                        <div class="panel panel-default">
                      <div class="panel-heading">
                          <g:set var="expkey" value="${g.rkey()}"/>
                            <span data-toggle="collapse" data-target="#${expkey}" data-bind="css: {in: changeTargetNodes}">
                                <g:if test="${group!='other'}">
                                    <span class="prompt">
                                    <g:enc>${namegroups[group][0]}</g:enc></span>
                                    <g:message code="to"/>
                                    <span class="prompt">
                                <g:enc>${namegroups[group][-1]}</g:enc>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span class="prompt"><g:enc>${namegroups.size()>1?'Other ':''}</g:enc><g:message code="matched.nodes.prompt" /></span>
                                </g:else>
                                <g:enc>(${namegroups[group].size()})</g:enc>
                                <b class="glyphicon glyphicon-chevron-${selectedNodes!=null ? 'down' : 'right'}"></b>
                            </span>
                        </div>
                        <div id="${enc(attr:expkey)}"  class="group_section panel-body collapse " data-bind="css: {in: changeTargetNodes}" >
                                <g:if test="${namegroups.size()>1}">
                                <div class="group_select_control" style="">

                                    <span class="btn btn-xs btn-simple btn-hover selectall" data-bind="click: function(){groupSelectAll($element)}" data-group="${enc(attr:group)}">
                                        <g:icon name="check"/>
                                        <g:message code="select.all" />
                                    </span>
                                    <span class="btn btn-xs btn-simple  btn-hover selectnone"  data-bind="click: function(){groupSelectNone($element)}" data-group="${enc(attr:group)}" >
                                        <g:icon name="unchecked"/>
                                        <g:message code="select.none" />
                                    </span>
                                </div>
                                </g:if>
                                <g:if test="${grouptags && grouptags[group]}">
                                    <g:render template="/framework/tagsummary" model="${[tagsummary:grouptags[group],action:[classnames:'label label-muted active autoclickable  obs_tag_group',onclick:'']]}"/>
                                </g:if>
                                    <g:each var="node" in="${nodemap.subMap(namegroups[group]).values()}" status="index">
                                        <g:set var="nkey" value="${g.rkey()}"/>
                                        <div class="checkbox node_select_checkbox" >
                                          <input id="${enc(attr:nkey)}"
                                                 type="checkbox"
                                                 data-ident="node"
                                                 name="extra.nodeIncludeName"
                                                 data-bind="checked: selectedNodes, enable: changeTargetNodes"
                                                 value="${enc(attr:node.nodename)}"

                                                 data-tag="${enc(attr:node.tags?.join(' '))}"

                                                 />
                                            <label for="${enc(attr:nkey)}"
                                                   class=" ${localNodeName && localNodeName == node.nodename ? 'server' : ''} node_ident"
                                                   id="${enc(attr:nkey)}_key">


                                                    <g:nodeIconStatusColor node="${node}" icon="true">
                                                        <g:nodeIcon node="${node}">
                                                            <i class="fas fa-hdd"></i>
                                                        </g:nodeIcon>
                                                    </g:nodeIconStatusColor>&nbsp;<span class="${unselectedNodes&& unselectedNodes.contains(node.nodename)?'node_unselected':''}"><g:enc>${node.nodename}</g:enc></span>
                                                    <g:nodeHealthStatusColor node="${node}" title="${node.attributes['ui:status:text']}">
                                                        <g:nodeStatusIcon node="${node}"/>
                                                    </g:nodeHealthStatusColor>

                                                 </label>

                                        </div>
                                    </g:each>
                            </div>
                        </div>
                    </g:each>
                    </div>
                </g:if>
                <g:else>
                    <g:each var="node" in="${nodes}" status="index">
                        <g:set var="nkey" value="${g.rkey()}"/>
                        <div>
                            <label for="${enc(attr:nkey)}"
                                   class=" ${localNodeName && localNodeName == node.nodename ? 'server' : ''} node_ident  checkbox-inline"
                                   id="${enc(attr:nkey)}_key">
                                <input id="${enc(attr:nkey)}"
                                       type="checkbox"
                                       name="extra.nodeIncludeName"
                                       data-ident="node"
                                       data-bind="checked: selectedNodes, enable: changeTargetNodes"
                                       value="${enc(attr:node.nodename)}"
                                       data-tag="${enc(attr:node.tags?.join(' '))}"
                                       /><g:enc>${node.nodename}</g:enc></label>

                        </div>
                    </g:each>
                </g:else>
                </g:if>
                    </div>
                <g:if test="${scheduledExecution.nodeFilterEditable || nodefilter == ''}">
                <div class="subfields nodeFilterFields ">
                    %{-- filter text --}%
                    <div class="">
                        <g:set var="filtvalue" value="${nodefilter}"/>

                        <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                            <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters.findAll { it.project == project }}"/>
                        </g:if>

                        <div id="nodefilterViewArea" data-ko-bind="nodeFilter" data-bind="visible: nodeFiltersVisible">
                        <div class="${emptyQuery ? 'active' : ''}" id="nodeFilterInline">
                            <div class="spacing">
                                <div class="">
                                    <g:form action="adhoc" class="form form-horizontal" name="searchForm" >
                                        <g:hiddenField name="max" value="${max}"/>
                                        <g:hiddenField name="offset" value="${offset}"/>
                                        <g:hiddenField name="formInput" value="true"/>

                                        <div>
                                            <span class=" input-group multiple-control-input-group" >
                                                <g:render template="/framework/nodeFilterInputGroup"
                                                          model="[filterFieldName: 'extra.nodefilter',filterset: filterset, filtvalue: filtvalue, filterName: filterName]"/>
                                            </span>
                                        </div>
                                    </g:form>

                                    <div class=" collapse" id="queryFilterHelp">
                                        <div class="help-block">
                                            <g:render template="/common/nodefilterStringHelp"/>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </div>

                        <div>
                                <div class="spacing text-warning" id="emptyerror"
                                     style="display: none"
                                     data-bind="visible: !loading() && !error() && (!total() || total()==0)">
                                    <span class="errormessage">
                                        <g:message code="no.nodes.selected.match.nodes.by.selecting.or.entering.a.filter" />
                                    </span>
                                </div>
                                <div class="spacing text-danger" id="loaderror2"
                                     style="display: none"
                                     data-bind="visible: error()">
                                    <i class="glyphicon glyphicon-warning-sign"></i>
                                    <span class="errormessage" data-bind="text: error()">

                                    </span>
                                </div>
                                <div data-bind="visible: total()>0 || loading()" class="well well-sm inline">
                                    <span data-bind="if: loading()" class="text-info">
                                        <i class="glyphicon glyphicon-time"></i>
                                        <g:message code="loading.matched.nodes" />
                                    </span>
                                    <span data-bind="if: !loading() && !error()">

                                        <span data-bind="messageTemplate: [ total(), nodesTitle() ]"><g:message code="count.nodes.matched" /></span>.

                                        <span data-bind="if: total()>maxShown()">
                                            <span data-bind="messageTemplate: [maxShown(), total()]" class="text-strong"><g:message code="count.nodes.shown" /></span>
                                        </span>
                                        <span class="pull-right">
                                            <a href="#" data-bind="attr: {href: nodesPageViewUrl }">
                                              <g:message code="view.in.nodes.page.prompt" />
                                          </a>
                                        </span>

                                    </span>
                                </div>
                                <span >
                                    <g:render template="/framework/nodesEmbedKO"/>
                                </span>
                        </div>
                    </div>



                        %{-- filter text --}%
                    </div>


                </div>
                    </g:if>
            </div>
            </div>
            <g:javascript>

                jQuery('div.jobmatchednodes').on( 'click','span.selectall', function (evt) {
                    jQuery(this).closest('.group_section').find('input').each(function (i,el) {
                        if (el.type == 'checkbox') {
                            el.checked = true;
                        }
                    });
                    jQuery(this).closest('.group_section').find('span.obs_tag_group').each(function (i,e) {
                        jQuery(e).data('tagselected', 'true');
                        jQuery(e).addClass('active');
                    });

                });

                jQuery('div.jobmatchednodes').on( 'click','span.selectnone', function (evt) {
                    jQuery(this).closest('.group_section').find('input').each(function (i,el) {
                        if (el.type == 'checkbox') {
                            el.checked = false;
                        }
                    });
                    jQuery(this).closest('.group_section').find('span.obs_tag_group').each(function (i,e) {
                        jQuery(e).data('tagselected', 'false');
                        jQuery(e).removeClass('active');
                    });

                });


                jQuery('div.jobmatchednodes').on( 'click', 'div.node_select_checkbox', function (evt) {
                    jQuery(this).closest('.group_section').find('span.obs_tag_group').each(function (i,el) {
                        jQuery(el).data('tagselected', 'false');
                        jQuery(el).removeClass('active');
                    });
                });

                jQuery('#doReplaceFilters').on( 'change', function (evt) {
                    var e = evt.target
                    jQuery('div.jobmatchednodes input').each(function (i,cb) {
                        if (cb.type == 'checkbox') {
                            jQuery(cb).prop('disabled',!e.checked)
                            if (!e.checked) {
                                cb.checked = true;
                            }
                        }
                    });

                    if(!e.checked){
                        jQuery('.nodeselectcount').each(function (i,e2) {
                            jQuery(e2).removeClass('text-info');
                            jQuery(e2).removeClass('text-danger');
                        });
                    }
                });


                /** reset focus on click, so that IE triggers onchange event*/
                jQuery('#doReplaceFilters').on( 'click', function (evt) {
                    jQuery(this).trigger('blur');
                    jQuery(this).trigger('focus');
                });

            </g:javascript>

    </div>
        </div>
    </section>

    <div class="error note" id="formerror" style="display:none">

    </div>

</div>

<g:if test="${!hideHead}">
<div class=" exec-options-footer container-fluid">
    <div class="row" >
        <div class="col-sm-12 form-inline">
            <g:render template="/scheduledExecution/execOptionsFormButtons" model="[scheduledExecution:scheduledExecution,hideCancel:hideCancel]"/>
        </div>
    </div>
</div>
</g:if>
</div>
</g:uploadForm>

<script lang="text/javascript">
    function init() {
        var pageParams = loadJsonData('pageParams');
        jQuery('body').on('click', '.nodefilterlink', function (evt) {
            evt.preventDefault();
            nodeFilter.selectNodeFilterLink(this);
        });
        jQuery('#nodesContent').on('click', '.closeoutput', function (evt) {
            evt.preventDefault();
            closeOutputArea();
        });


        //setup node filters knockout bindings
        var filterParams = loadJsonData('filterParamsJSON');
        let kocontrollers={}
        var nodeFilter

        <g:if test="${scheduledExecution.nodeFilterEditable || nodefilter == ''}">
        var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
        nodeFilter = new NodeFilters(
                appLinks.frameworkAdhoc,
                appLinks.scheduledExecutionCreate,
                appLinks.frameworkNodes,
                Object.assign(filterParams, {
                    nodeSummary:nodeSummary,
                    view: 'embed',
                    maxShown: 100,
                    emptyMode: 'blank',
                    project: pageParams.project,
                    nodesTitleSingular: message('Node'),
                    nodesTitlePlural: message('Node.plural')
                }));

            // ko.applyBindings(nodeFilter, document.getElementById('nodefilterViewArea'));
        //show selected named filter
        nodeFilter.filterName.subscribe(function (val) {
            if (val) {
                jQuery('a[data-node-filter-name]').removeClass('active');
                jQuery('a[data-node-filter-name=\'' + val + '\']').addClass('active');
            }
        });

        nodeSummary.reload();
        nodeFilter.updateMatchedNodes();

        var tmpfilt = {};
        jQuery.data( tmpfilt, "node-filter-name", "" );
        jQuery.data( tmpfilt, "node-filter", "${nodefilter}" );
        nodeFilter.selectNodeFilterLink(tmpfilt);

        kocontrollers.nodeFilter = nodeFilter

        </g:if>
        let hasSelectedNodes=${enc(js:selectedNodes!=null && selectedNodes)};
        let hasSelectedByDefault=${enc(js:scheduledExecution.hasNodesSelectedByDefault())};
        let selectedNodes=hasSelectedNodes?loadJsonData('selectedNodesJson'):hasSelectedByDefault?loadJsonData('allNodesJson'):[];
        kocontrollers.runformoptions = new JobRunFormOptions({
            debug:${enc(js:scheduledExecution?.loglevel=='DEBUG')},
            changeTargetNodes:hasSelectedNodes||!hasSelectedByDefault,
            canOverrideFilter:${enc(js:scheduledExecution.nodeFilterEditable|| nodefilter == '')},
            nodeOverride: "${enc(js:!nodesetvariables && nodes?'cherrypick':'filter')}",
            selectedNodes: selectedNodes,
            hasDynamicFilter: ${enc(js:!!nodesetvariables)},
            allNodes:loadJsonData('allNodesJson'),
            hasSelectedNodes: hasSelectedNodes,
            groups:loadJsonData('namegroupsJson'),
            grouptags:loadJsonData('namegrouptagsJson'),
            nodeFilter: nodeFilter
        })
        if (typeof (nodeFilter) !== 'undefined') {
            kocontrollers.runformoptions.isNodeFilterVisible.subscribe(nodeFilter.nodeFiltersVisible)
            nodeFilter.nodeFiltersVisible(kocontrollers.runformoptions.isNodeFilterVisible())
        }

        jQuery('div.jobmatchednodes').on( 'click', 'span.obs_tag_group', function (evt) {
            var ischecked = jQuery(this).data('tagselected') != 'false';
            jQuery(this).data('tagselected', ischecked ? 'false' : 'true');
            if (!ischecked) {
                jQuery(this).addClass('active');
            } else {
                jQuery(this).removeClass('active');
            }
            jQuery(this).closest('.group_section').find('input[data-tag~="' + jQuery(this).data('tag') + '"]').each(function (i,el) {
                if (el.type == 'checkbox') {
                    el.checked = !ischecked;
                }
                let selectedNodes = kocontrollers.runformoptions.selectedNodes();
                var index = selectedNodes.indexOf(el.value);
                if (index > -1) {
                    if (!el.checked) {selectedNodes.splice(index, 1);}
                } else {
                    if (el.checked) { selectedNodes.push(el.value)}
                }
                kocontrollers.runformoptions.selectedNodes(selectedNodes)
            });
            jQuery(this).closest('.group_section').find('span.obs_tag_group[data-tag="' + jQuery(this).data('tag') + '"]').each(function (i,el) {
                jQuery(el).data('tagselected', ischecked ? 'false' : 'true');
                if (!ischecked) {
                    jQuery(el).addClass('active');
                } else {
                    jQuery(el).removeClass('active');
                }
            });

        });

        initKoBind('#exec_options_form', kocontrollers, /*'execform'*/)
    }
    jQuery(document).ready(init);
</script>
<content tag="footScripts">
    <asset:stylesheet src="library/bootstrap-datetimepicker.min.css" />
    <asset:javascript src="scheduler.js" />
</content tag="footScripts">

<asset:stylesheet src="library/bootstrap-datetimepicker.min.css" />
<asset:javascript src="scheduler.js" />
