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
<%@ page import="rundeck.User;" %>
<div class="row">
<div class="col-sm-12 ">

<g:form controller="scheduledExecution" method="post" useToken="true"
        params="[project:scheduledExecution.project]" class="form-horizontal" role="form">
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

<input id='runAtTime' type='hidden' name='runAtTime' value='' />

<div class="panel panel-default panel-tab-content panel-modal-content">
<g:if test="${!hideHead}">
    <div class="panel-heading">
        <div class="row">
            <tmpl:showHead scheduledExecution="${scheduledExecution}" iconName="icon-job"
                           runPage="true" jobDescriptionMode="collapsed"/>
        </div>
    </div>
</g:if>
    <g:set var="project" value="${scheduledExecution?.project ?: params.project?:request.project?: projects?.size() == 1 ? projects[0].name : ''}"/>
    <g:embedJSON id="filterParamsJSON" data="${[filterName: params.filterName, filter: query?.filter, filterAll: params.showall in ['true', true]]}"/>
<script lang="text/javascript">
    function init() {
        var pageParams = loadJsonData('pageParams');
        jQuery('body').on('click', '.nodefilterlink', function (evt) {
            evt.preventDefault();
            nodeFilter.selectNodeFilterLink(this);
            $('filterradio').checked=true;
        });
        jQuery('#nodesContent').on('click', '.closeoutput', function (evt) {
            evt.preventDefault();
            closeOutputArea();
        });


        //setup node filters knockout bindings
        var filterParams = loadJsonData('filterParamsJSON');
        <g:if test="${scheduledExecution.nodeFilterEditable || nodefilter == ''}">
        var nodeSummary = new NodeSummary({baseUrl:appLinks.frameworkNodes});
        var nodeFilter = new NodeFilters(
                appLinks.frameworkAdhoc,
                appLinks.scheduledExecutionCreate,
                appLinks.frameworkNodes,
                Object.extend(filterParams, {
                    nodeSummary:nodeSummary,
                    view: 'embed',
                    maxShown: 100,
                    emptyMode: 'blank',
                    project: pageParams.project,
                    nodesTitleSingular: message('Node'),
                    nodesTitlePlural: message('Node.plural')
                }));

            ko.applyBindings(nodeFilter, document.getElementById('nodefilterViewArea'));
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

        </g:if>
    }
    jQuery(document).ready(init);
</script>
    <div class=" collapse" id="queryFilterHelp">
        <div class="help-block">
            <g:render template="/common/nodefilterStringHelp"/>
        </div>
    </div>
<div class="list-group list-group-tab-content">
<div class="list-group-item">
<div class="row">
<div class="${hideHead?'col-sm-9':'col-sm-12'}">
    <g:render template="editOptions" model="${[scheduledExecution:scheduledExecution, selectedoptsmap:selectedoptsmap, selectedargstring:selectedargstring,authorized:authorized,jobexecOptionErrors:jobexecOptionErrors, optiondependencies: optiondependencies, dependentoptions: dependentoptions, optionordering: optionordering]}"/>

    <div class="form-group" style="${wdgt.styleVisible(if: nodesetvariables && !failedNodes || nodesetempty || nodes)}">
    <div class="col-sm-2 control-label text-form-label">
        <g:message code="Node.plural" />
    </div>


    <div class="col-sm-10">
        <g:if test="${nodesetvariables && !failedNodes}">
            %{--show node filters--}%
            <div>
                <span class="query form-control-static ">
                   <span class="queryvalue text"><g:enc>${nodefilter}</g:enc></span>
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
                   value="${failedNodes? failedNodes.split(',').findAll{it}:selectedNodes!=null? selectedNodes.split(',').findAll{it}:null}"/>

            <div class="row">
                <div class="col-sm-12 checkbox">
                    <label >
                    <input name="extra._replaceNodeFilters" value="true" type="checkbox"
                           data-toggle="collapse"
                           data-target="#nodeSelect"
                        ${selectedNodes!=null?'checked':''}
                           id="doReplaceFilters"/>
                    <g:message code="change.the.target.nodes" />
                    <g:if test="${selectedNodes || nodes}">
                        (<span class="nodeselectcount"><g:enc>${selectedNodes!=null?selectedNodes.size():nodes.size()}</g:enc></span>)
                    </g:if>
                    </label>
                </div>

            </div>
            <div class="container">
            <div class=" matchednodes embed jobmatchednodes group_section collapse ${selectedNodes!=null? 'in' : ''}" id="nodeSelect">
                <%--
                 split node names into groups, in several patterns
                  .*\D(\d+)
                  (\d+)\D.*
                --%>
                <g:if test="${!nodesetvariables && nodes}">
                <g:if test="${namegroups}">
                    <label for=" ">
                    <div class=" group_select_control" style="${wdgt.styleVisible(if: selectedNodes !=null)}">
                        <input id="cherrypickradio"
                               type="radio"
                               name="extra.nodeoverride"
                                checked="checked"
                                value="cherrypick"
                               />
                        <g:message code="select.prompt" /> (<span class="nodeselectcount"><g:enc>${selectedNodes!=null?selectedNodes.size():nodes.size()}</g:enc></span>)
                        <span class="textbtn textbtn-default textbtn-on-hover selectall"><g:message code="all" /></span>
                        <span class="textbtn textbtn-default textbtn-on-hover selectnone"><g:message code="none" /></span>
                        <g:if test="${tagsummary}">
                            <g:render template="/framework/tagsummary"
                                      model="${[tagsummary:tagsummary,action:[classnames:'tag active textbtn obs_tag_group',onclick:'']]}"/>
                        </g:if>

                    </div>
                    </label>
                    <g:each in="${namegroups.keySet().sort()}" var="group">
                        <div class="panel panel-default">
                      <div class="panel-heading">
                          <g:set var="expkey" value="${g.rkey()}"/>
                            <g:expander key="${expkey}" open="${selectedNodes!=null?'true':'false'}">
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
                            </g:expander>
                        </div>
                        <div id="${enc(attr:expkey)}" style="${wdgt.styleVisible(if: selectedNodes!=null)}" class="group_section panel-body">
                                <g:if test="${namegroups.size()>1}">
                                <div class="group_select_control" style="display:none">
                                    <g:message code="select.prompt" />
                                    <span class="textbtn textbtn-default textbtn-on-hover selectall" >All</span>
                                    <span class="textbtn textbtn-default textbtn-on-hover selectnone" >None</span>
                                    <g:if test="${grouptags && grouptags[group]}">
                                        <g:render template="/framework/tagsummary" model="${[tagsummary:grouptags[group],action:[classnames:'tag active textbtn  obs_tag_group',onclick:'']]}"/>
                                    </g:if>
                                </div>
                                </g:if>
                                    <g:each var="node" in="${nodemap.subMap(namegroups[group]).values()}" status="index">
                                        <g:set var="nkey" value="${g.rkey()}"/>
                                        <div>
                                            <label for="${enc(attr:nkey)}"
                                                   class=" ${localNodeName && localNodeName == node.nodename ? 'server' : ''} node_ident  checkbox-inline"
                                                   id="${enc(attr:nkey)}_key">
                                            <input id="${enc(attr:nkey)}"
                                                   type="checkbox"
                                                   name="extra.nodeIncludeName"
                                                   value="${enc(attr:node.nodename)}"
                                                   ${selectedNodes!=null ? '':'disabled' }
                                                   data-tag="${enc(attr:node.tags?.join(' '))}"
                                                    ${(null== selectedNodes||selectedNodes.contains(node.nodename))?'checked':''}
                                                   /><g:enc>${node.nodename}</g:enc></label>

                                        </div>
                                    </g:each>
                            </div>
                        </div>
                    </g:each>

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
                                       value="${enc(attr:node.nodename)}"
                                       disabled="true"
                                       data-tag="${enc(attr:node.tags?.join(' '))}"
                                       checked="true"/><g:enc>${node.nodename}</g:enc></label>

                        </div>
                    </g:each>
                </g:else>
                </g:if>
                <g:if test="${scheduledExecution.nodeFilterEditable || nodefilter == ''}">
                <div class="subfields nodeFilterFields ">
                    %{-- filter text --}%
                    <div class="">
                        <g:set var="filtvalue" value="${nodefilter}"/>
                        <label for="filterradio" style="display: block">
                    <input id="filterradio"
                           type="radio"
                           name="extra.nodeoverride"
                        ${(!nodesetvariables && nodes)?'':'checked=true'}
                           value="filter"
                    />
                        <span>
                    <g:if test="${!nodesetvariables && nodes}"><g:message code="or"/> </g:if>
                            <g:message code="job.run.override.node"/>: </span>
                    <g:if test="${session.user && User.findByLogin(session.user)?.nodefilters}">
                        <g:set var="filterset" value="${User.findByLogin(session.user)?.nodefilters.findAll{it.project == project}}"/>
                    </g:if>

                    <div id="nodefilterViewArea">
                        <div class="${emptyQuery ? 'active' : ''}" id="nodeFilterInline">
                            <div class="spacing">
                                <div class="">
                                    <g:form action="adhoc" class="form form-horizontal" name="searchForm" >
                                        <g:hiddenField name="max" value="${max}"/>
                                        <g:hiddenField name="offset" value="${offset}"/>
                                        <g:hiddenField name="formInput" value="true"/>



                                        <div class="form-group">
                                            <div class="col-sm-10">
                                                <span class=" input-group" >
                                                    <g:render template="/framework/nodeFilterInputGroup"
                                                              model="[filterFieldName: 'extra.nodefilter',filterset: filterset, filtvalue: filtvalue, filterName: filterName]"/>
                                                </span>
                                            </div>
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

                        <div class="row row-space">
                            <div class="col-sm-10">
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
                                            <span data-bind="messageTemplate: [maxShown(), total()]" class="text-muted"><g:message code="count.nodes.shown" /></span>
                                        </span>
                                        <a class="textbtn textbtn-default pull-right" data-bind="click: nodesPageView">
                                            <g:message code="view.in.nodes.page.prompt" />
                                        </a>
                                    </span>
                                </div>
                                <span >
                                    <g:render template="/framework/nodesEmbedKO"/>
                                </span>
                            </div>
                        </div>
                    </div>



                </label>
                        %{-- filter text --}%
                    </div>


                </div>
                    </g:if>
            </div>
            </div>
            <g:javascript>
                var updateSelectCount = function (evt) {
                    var count = 0;
                    $$('.node_ident input[type=checkbox]').each(function (e2) {
                        if (e2.checked) {
                            count++;
                        }
                    });
                    $$('.nodeselectcount').each(function (e2) {
                        setText($(e2), count + '');
                        $(e2).removeClassName('text-info');
                        $(e2).removeClassName('text-danger');
                        $(e2).addClassName(count>0?'text-info':'text-danger');
                    });
                };
                $$('.node_ident input[type=checkbox]').each(function (e) {
                    Event.observe(e, 'change', function (evt) {
                      Event.fire($('nodeSelect'), 'nodeset:change');
                    });
                });
                Event.observe($('nodeSelect'), 'nodeset:change', updateSelectCount);
                $$('div.jobmatchednodes span.textbtn.selectall').each(function (e) {
                    Event.observe(e, 'click', function (evt) {
                        $(e).up('.group_section').select('input').each(function (el) {
                            if (el.type == 'checkbox') {
                                el.checked = true;
                            }
                        });
                        $(e).up('.group_section').select('span.textbtn.obs_tag_group').each(function (e) {
                            $(e).setAttribute('data-tagselected', 'true');
                            $(e).addClassName('active');
                        });
                        Event.fire($('nodeSelect'), 'nodeset:change');
                    });
                });
                $$('div.jobmatchednodes span.textbtn.selectnone').each(function (e) {
                    Event.observe(e, 'click', function (evt) {
                        $(e).up('.group_section').select('input').each(function (el) {
                            if (el.type == 'checkbox') {
                                el.checked = false;
                            }
                        });
                        $(e).up('.group_section').select('span.textbtn.obs_tag_group').each(function (e) {
                            $(e).setAttribute('data-tagselected', 'false');
                            $(e).removeClassName('active');
                        });
                        Event.fire($('nodeSelect'), 'nodeset:change');
                    });
                });
                $$('div.jobmatchednodes span.textbtn.obs_tag_group').each(function (e) {
                    Event.observe(e, 'click', function (evt) {
                        var ischecked = e.getAttribute('data-tagselected') != 'false';
                        e.setAttribute('data-tagselected', ischecked ? 'false' : 'true');
                        if (!ischecked) {
                            $(e).addClassName('active');
                        } else {
                            $(e).removeClassName('active');
                        }
                        $(e).up('.group_section').select('input[data-tag~="' + e.getAttribute('data-tag') + '"]').each(function (el) {
                            if (el.type == 'checkbox') {
                                el.checked = !ischecked;
                            }
                        });
                        $(e).up('.group_section').select('span.textbtn.obs_tag_group[data-tag="' + e.getAttribute('data-tag') + '"]').each(function (el) {
                            el.setAttribute('data-tagselected', ischecked ? 'false' : 'true');
                            if (!ischecked) {
                                $(el).addClassName('active');
                            } else {
                                $(el).removeClassName('active');
                            }
                        });
                        Event.fire($('nodeSelect'), 'nodeset:change');
                    });
                });

                Event.observe($('doReplaceFilters'), 'change', function (evt) {
                    var e = evt.element();
                    $$('div.jobmatchednodes input').each(function (cb) {
                        if (cb.type == 'checkbox') {
                            [cb].each(e.checked ? Field.enable : Field.disable);
                            if (!e.checked) {
                                $$('.group_select_control').each(Element.hide);
                                cb.checked = true;
                            } else {
                                $$('.group_select_control').each(Element.show);
                            }
                        }
                    });
                    Event.fire($('nodeSelect'), 'nodeset:change');
                    if(!e.checked){
                        $$('.nodeselectcount').each(function (e2) {
                            $(e2).removeClassName('text-info');
                            $(e2).removeClassName('text-danger');
                        });
                    }
                });


                /** reset focus on click, so that IE triggers onchange event*/
                Event.observe($('doReplaceFilters'), 'click', function (evt) {
                    this.blur();
                    this.focus();
                });

            </g:javascript>
            <g:if test="${scheduledExecution.hasNodesSelectedByDefault()}">
                <g:javascript>
                    Event.fire($('nodeSelect'), 'nodeset:change');
                </g:javascript>
            </g:if>

    </div>
    </div>

    <div class="error note" id="formerror" style="display:none">

    </div>
</div>
<g:if test="${hideHead}">
<div class="col-sm-3">
    <div id="formbuttons">
        <g:if test="${!hideCancel}">
            <g:actionSubmit id="execFormCancelButton" value="Cancel" class="btn btn-default"/>
        </g:if>
        <div class="pull-right">
            <div title="${scheduledExecution.hasExecutionEnabled() ? '':g.message(code: 'disabled.job.run')}"
                  class="has_tooltip"
                  data-toggle="tooltip"
                  data-placement="auto right"
            >%{--Extra div because attr disabled will cancel tooltip from showing --}%
                <button type="submit"
                        name="_action_runJobNow"
                        id="execFormRunButton"
                        ${scheduledExecution.hasExecutionEnabled() ? '':'disabled' }
                        class=" btn btn-success">
                    <g:message code="run.job.now" />
                    <b class="glyphicon glyphicon-play"></b>
                </button>
                <a tabindex="0" role="button"
                        id="showScheduler"
                        ${scheduledExecution.hasExecutionEnabled() ? '':'disabled' }
                        class=" btn btn-default"
                        data-toggle="popover" title="Set start time" data-trigger="click"
                        data-placement="auto left" data-container="body" data-html="true"
                        data-trigger="focus" data-content="<div id='scheduler'>
                                <div class='input-group date' id='datetimepicker'>
                                    <input type='text' class='form-control' />
                                    <span class='input-group-addon'>
                                        <span class='glyphicon glyphicon-calendar'></span>
                                    </span>
                                </div>
                                <div id='dateAlert' class='alert alert-warning alert-block fade' style='display: none'>
                                    ${message(code:"the.time.must.be.in.the.future")}
                                </div>
                                <button type='submit'
                                        id='scheduleSubmitButton'
                                        name='_action_runJobLater'
                                        class=' btn btn-success schedule-button'>
                                    ${message(code:'schedule.job')}
                                    <b class='glyphicon glyphicon-time'></b>
                                </button>
                            </div>">
                    <g:message code="run.job.later" />
                    <b class="glyphicon glyphicon-time"></b>
                </a>
            </div>
        </div>
        <div class="clearfix">
        </div>
        <div class="pull-right">
            <label class="control-label">
                <g:checkBox id="followoutputcheck" name="follow"
                            checked="${defaultFollow || params.follow == 'true'}"
                            value="true"/>
                <g:message code="job.run.watch.output"/>
            </label>
        </div>
    </div>
</div>
</g:if>
</div>
</div>
</div>
<g:if test="${!hideHead}">
<div class="panel-footer">
    <div class="row" >
        <div class="col-sm-12 form-inline" id="formbuttons">
            <g:if test="${!hideCancel}">
                <g:actionSubmit id="execFormCancelButton" value="Cancel" class="btn btn-default"/>
            </g:if>
            <div title="${scheduledExecution.hasExecutionEnabled() ? '':g.message(code: 'disabled.job.run')}"
                  class="form-group has_tooltip"
                  data-toggle="tooltip"
                  data-placement="auto right"
            >%{--Extra div because attr disabled will cancel tooltip from showing --}%
                <button type="submit"
                        name="_action_runJobNow"
                        id="execFormRunButton"
                        ${scheduledExecution.hasExecutionEnabled() ? '':'disabled' }
                        class=" btn btn-success">
                    <i class="glyphicon glyphicon-play"></i>
                    <g:message code="run.job.now" />
                </button>
                <a tabindex="0" role="button"
                        id="showScheduler"
                        ${scheduledExecution.hasExecutionEnabled() ? '':'disabled' }
                        class=" btn btn-default"
                        data-toggle="popover" title="Set start time" data-trigger="click"
                        data-placement="auto bottom" data-container="#formbuttons" data-html="true"
                        data-trigger="focus" data-content="<div id='scheduler'>
                                <div class='input-group date' id='datetimepicker'>
                                    <input type='text' class='form-control' />
                                    <span class='input-group-addon'>
                                        <span class='glyphicon glyphicon-calendar'></span>
                                    </span>
                                </div>
                                <div id='dateAlert' class='alert alert-warning alert-block fade' style='display: none'>
                                    ${message(code:"the.time.must.be.in.the.future")}
                                </div>
                                <button type='submit'
                                        id='scheduleAjaxButton'
                                        class=' btn btn-success schedule-button'>
                                    <i class='glyphicon glyphicon-time'></i>
                                    ${message(code:'schedule.job')}
                                </button>
                            </div>">
                    <i class="glyphicon glyphicon-time"></i>
                    <g:message code="run.job.later" />
                </a>

            </div>
            <div class="checkbox-inline">
                <label>
                    <g:checkBox id="followoutputcheck"
                                name="follow"
                                checked="${defaultFollow || params.follow == 'true'}"
                                value="true"/>
                    <g:message code="job.run.watch.output"/>
                </label>
            </div>
        </div>
    </div>
</div>
</g:if>
</div>%{--/.panel--}%
</g:form>
</div> %{--/.col--}%
</div> %{--/.row--}%

<content tag="footScripts">
    <asset:stylesheet src="bootstrap-datetimepicker.min.css" />
    <asset:javascript src="scheduler.js" />
</content tag="footScripts">

<asset:stylesheet src="bootstrap-datetimepicker.min.css" />
<asset:javascript src="scheduler.js" />
