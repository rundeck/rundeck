
<div class="row">
<div class="col-sm-12 ">
<g:form controller="scheduledExecution" method="post" action="runJobNow" useToken="true"
        params="[project:scheduledExecution.project]" class="form-horizontal" role="form">
<div class="panel panel-default panel-tab-content">
<g:if test="${!hideHead}">
    <div class="panel-heading">
        <div class="row">
            <tmpl:showHead scheduledExecution="${scheduledExecution}" iconName="icon-job"
                           subtitle="Choose Execution Options" runPage="true" jobDescriptionMode="collapsed"/>
        </div>
    </div>
</g:if>
<div class="list-group list-group-tab-content">
<div class="list-group-item">
<div class="row">
<div class="${hideHead?'col-sm-9':'col-sm-12'}">
    <g:render template="editOptions" model="${[scheduledExecution:scheduledExecution, selectedoptsmap:selectedoptsmap, selectedargstring:selectedargstring,authorized:authorized,jobexecOptionErrors:jobexecOptionErrors, optiondependencies: optiondependencies, dependentoptions: dependentoptions, optionordering: optionordering]}"/>
    <div class="form-group" style="${wdgt.styleVisible(if: nodesetvariables && !failedNodes || nodesetempty || nodes)}">
    <div class="col-sm-2 control-label text-form-label">
        Nodes
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
        <g:elseif test="${nodes}">
            <g:set var="selectedNodes"
                   value="${failedNodes? failedNodes.split(','):selectedNodes? selectedNodes.split(','):null}"/>
            <div class="container">
            <div class="row">
                <div class="col-sm-12 checkbox">
                <input name="extra._replaceNodeFilters" value="true" type="checkbox"
                        data-toggle="collapse"
                        data-target="#nodeSelect"
                    ${selectedNodes?'checked':''}
                              id="doReplaceFilters"/> <label for="doReplaceFilters">Change the Target Nodes
                (<span class="nodeselectcount"><g:enc>${selectedNodes?selectedNodes.size():nodes.size()}</g:enc></span>)</label>
                </div>

            </div>
            </div>
            <div class=" matchednodes embed jobmatchednodes group_section collapse ${selectedNodes ? 'in' : ''}" id="nodeSelect">
                <%--
                 split node names into groups, in several patterns
                  .*\D(\d+)
                  (\d+)\D.*
                --%>
                <g:if test="${namegroups}">
                    <div class=" group_select_control" style="${wdgt.styleVisible(if: selectedNodes)}">
                        Select:
                        <span class="textbtn textbtn-default textbtn-on-hover selectall">All</span>
                        <span class="textbtn textbtn-default textbtn-on-hover selectnone">None</span>
                        <g:if test="${tagsummary}">
                            <g:render template="/framework/tagsummary"
                                      model="${[tagsummary:tagsummary,action:[classnames:'tag active textbtn obs_tag_group',onclick:'']]}"/>
                        </g:if>
                    </div>
                    <g:each in="${namegroups.keySet().sort()}" var="group">
                        <div class="panel panel-default">
                      <div class="panel-heading">
                          <g:set var="expkey" value="${g.rkey()}"/>
                            <g:expander key="${expkey}" open="${selectedNodes?'true':'false'}">
                                <g:if test="${group!='other'}">
                                    <span class="prompt">
                                    <g:enc>${namegroups[group][0]}</g:enc></span>
                                    to
                                    <span class="prompt">
                                <g:enc>${namegroups[group][-1]}</g:enc>
                                    </span>
                                </g:if>
                                <g:else>
                                    <span class="prompt"><g:enc>${namegroups.size()>1?'Other ':''}</g:enc>Matched Nodes</span>
                                </g:else>
                                <g:enc>(${namegroups[group].size()})</g:enc>
                            </g:expander>
                        </div>
                        <div id="${enc(attr:expkey)}" style="${wdgt.styleVisible(if: selectedNodes)}" class="group_section panel-body">
                                <g:if test="${namegroups.size()>1}">
                                <div class="group_select_control" style="display:none">
                                    Select:
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
                                                   ${selectedNodes ? '':'disabled' }
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
        </g:elseif>
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
            <button type="submit"
                    id="execFormRunButton"
                    class=" btn btn-success ">
                    <g:message code="run.job.now" />
                <b class="glyphicon glyphicon-play"></b>
            </button>
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
        <div class="col-sm-12" id="formbuttons">
            <g:if test="${!hideCancel}">
                <g:actionSubmit id="execFormCancelButton" value="Cancel" class="btn btn-default"/>
            </g:if>
            <button type="submit"
                    id="execFormRunButton"
                    class=" btn btn-success">
                <i class="glyphicon glyphicon-play"></i>
                <g:message code="run.job.now" />
            </button>
            <label class="checkbox-inline">
                <g:checkBox id="followoutputcheck"
                            name="follow"
                            checked="${defaultFollow || params.follow == 'true'}"
                            value="true"/>
                <g:message code="job.run.watch.output"/>
            </label>
        </div>
    </div>
</div>
</g:if>
</div>%{--/.panel--}%
</g:form>
</div> %{--/.col--}%
</div> %{--/.row--}%
