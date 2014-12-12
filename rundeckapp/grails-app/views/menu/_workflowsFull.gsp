<%@ page import="rundeck.User; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
<%--
    workflowsFull.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: Feb 9, 2010 11:14:07 AM
    $Id$
 --%>

<g:timerStart key="_workflowsFull.gsp"/>
<g:timerStart key="head"/>
<%-- define form display conditions --%>

<g:set var="rkey" value="${rkey?:g.rkey()}"/>

<g:if test="${session.user && User.findByLogin(session.user)?.jobfilters}">
    <g:set var="filterset" value="${User.findByLogin(session.user)?.jobfilters}"/>
</g:if>

<div id="${enc(attr:rkey)}wffilterform">
    <g:render template="/common/messages"/>
    <g:set var="wasfiltered" value="${paginateParams?.keySet().grep(~/(?!proj).*Filter|groupPath|idlist$/)}"/>
    <g:if test="${params.createFilters}">
        <span class="note help">
            Enter filter parameters below and click "save this filter" to set a name and save it.
        </span>
    </g:if>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter?true:false}"/>
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>

            <td style="text-align:left;vertical-align:top;width:200px; ${wdgt.styleVisible(if:filtersOpen)}" id="${enc(attr:rkey)}filter" class="wffilter" >

            <g:form action="jobs" params="[project:params.project]" method="POST" class="form" useToken="true">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <g:hiddenField name="project" value="${params.project}"/>
                <span class="textbtn textbtn-default obs_filtertoggle">
                    Filter
                    <b class="glyphicon glyphicon-chevron-down"></b>
                </span>
                <g:if test="${!filterName}">
                    <a class="btn btn-xs pull-right btn-success"
                          data-toggle="modal"
                          href="#saveFilterModal" title="Click to save this filter with a name">
                        <i class="glyphicon glyphicon-plus"></i> save this filter&hellip;
                    </a>
                </g:if>
                <g:else >
                    <div class="filterdef saved clear">
                                    <span class="prompt"><g:enc>${filterName}</g:enc></span>
                    <a class="btn btn-xs btn-link btn-danger pull-right" data-toggle="modal"
                          href="#deleteFilterModal" title="Click to delete this saved filter">
                        <b class="glyphicon glyphicon-remove"></b>
                        delete&hellip;
                    </a>
                    </div>
                </g:else>
                <g:render template="/common/queryFilterManagerModal" model="${[rkey:rkey,filterName:filterName,
                        filterset:filterset,update:rkey+'wffilterform',
                        deleteActionSubmit:'deleteJobfilter',
                        storeActionSubmit:'storeJobfilter']}"/>
                
                <div class="filter">

                            <g:hiddenField name="max" value="${max}"/>
                            <g:hiddenField name="offset" value="${offset}"/>
                            <g:if test="${params.idlist}">
                                <div class="form-group">
                                    <label for="${enc(attr:rkey)}idlist"><g:message code="jobquery.title.idlist"/></label>:
                                    <g:textField name="idlist" id="${rkey}idlist" value="${params.idlist}"
                                                 class="form-control" />
                                </div>
                            </g:if>
                            <div class="form-group">
                                <label for="${enc(attr:rkey)}jobFilter"><g:message code="jobquery.title.jobFilter"/></label>:
                                <g:textField name="jobFilter" id="${rkey}jobFilter" value="${params.jobFilter}"
                                             class="form-control" />
                            </div>

                            <div class="form-group">
                                <label for="${enc(attr:rkey)}groupPath"><g:message code="jobquery.title.groupPath"/></label>:
                                <g:textField name="groupPath" id="${rkey}groupPath" value="${params.groupPath}"
                                             class="form-control"/>
                            </div>

                            <div class="form-group">
                                <label for="${enc(attr:rkey)}descFilter"><g:message code="jobquery.title.descFilter"/></label>:
                                <g:textField name="descFilter" id="${rkey}descFilter" value="${params.descFilter}"
                                             class="form-control"/>
                            </div>


                            <div class="form-group">
                                    <g:actionSubmit  value="Filter" name="filterAll" controller='menu' action='jobs'  class="btn btn-primary btn-sm"/>
                                    <g:actionSubmit  value="Clear" name="clearFilter" controller='menu' action='jobs' class="btn btn-default btn-sm"/>
                            </div>
                </div>
            </g:form>

            </td>
            <td style="text-align:left;vertical-align:top;" id="${enc(attr:rkey)}wfcontent" class="wfcontent">

                <div class="jobscontent head">
    <g:if test="${!params.compact}">
        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
        <div class=" pull-right" >
            <div class="btn-group">
            <button type="button" class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown">
                Create <g:message code="domain.ScheduledExecution.title"/>
                <span class="caret"></span>
            </button>
            <ul class="dropdown-menu pull-right" role="menu">
                <li><g:link controller="scheduledExecution" action="create"
                    params="[project: params.project ?: request.project]"
                            class="">
                    <i class="glyphicon glyphicon-plus"></i>
                    New <g:message
                            code="domain.ScheduledExecution.title"/>&hellip;</g:link></li>
                <li class="divider">
                </li>
                <li>
                    <g:link controller="scheduledExecution" action="upload"
                            params="[project: params.project ?: request.project]"
                            class="">
                        <i class="glyphicon glyphicon-upload"></i>
                        Upload Definition&hellip;
                    </g:link>
                </li>
            </ul>
            </div>
        </div>
        </auth:resourceAllowed>
    </g:if>

                <g:if test="${wasfiltered}">
                    <div>
                    <g:if test="${!params.compact}">
                        <span class="h4"><g:enc>${totalauthorized}</g:enc> <g:message code="domain.ScheduledExecution.title"/>s</span>
                            matching filter:
                    </g:if>

                    <g:if test="${filterset}">
                        <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        <!--<span class="info note">Filter:</span>-->
                    </g:if>
                    </div>

                            <span title="Click to modify filter" class="info textbtn textbtn-default query obs_filtertoggle"  id='${rkey}filter-toggle'>
                                <g:each in="${wasfiltered.sort()}" var="qparam">
                                    <span class="querykey"><g:message code="jobquery.title.${qparam}"/></span>:

                                    <g:if test="${paginateParams[qparam] instanceof java.util.Date}">
                                        <span class="queryvalue date" title="${enc(attr:paginateParams[qparam].toString())}">
                                            <g:relativeDate atDate="${paginateParams[qparam]}"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <span class="queryvalue text">
                                            ${g.message(code:'jobquery.title.'+qparam+'.label.'+paginateParams[qparam].toString(),default:enc(html:paginateParams[qparam].toString()).toString())}
                                        </span>
                                    </g:else>

                                </g:each>

                                <b class="glyphicon glyphicon-chevron-right"></b>
                            </span>
                </g:if>
                <g:else>
                    <g:if test="${!params.compact}">
                    <span class="h4"><g:message code="domain.ScheduledExecution.title"/>s (<g:enc>${totalauthorized}</g:enc>)</span>
                    </g:if>

                    <span class="textbtn textbtn-default obs_filtertoggle"  id="${enc(attr:rkey)}filter-toggle">
                        Filter
                        <b class="glyphicon glyphicon-chevron-${wasfiltered?'down':'right'}"></b>
                    </span>
                    <g:if test="${filterset}">
                        <span style="margin-left:10px;">
                            <span class="info note">Choose a Filter:</span>
                            <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        </span>
                    </g:if>
                </g:else>
                    <span class="textbtn textbtn-default obs_expand_all">
                        Expand All
                    </span>
                    <span class="textbtn textbtn-default obs_collapse_all">
                        Collapse All
                    </span>
                    <div class="clear"></div>
                </div>

                <g:if test="${flash.savedJob}">
                    <div class="newjob">
                    <span class="popout message note" style="background:white">
                        <g:enc>${flash.savedJobMessage?:'Saved changes to Job'}</g:enc>:
                        <g:link controller="scheduledExecution" action="show" id="${flash.savedJob.id}"
                                params="[project: params.project ?: request.project]"><g:enc>${flash.savedJob.generateFullName()}</g:enc></g:link>
                    </span>
                    </div>
                    <g:javascript>
                        fireWhenReady('jobrow_${enc(js:flash.savedJob.id)}',doyft.curry('jobrow_${enc(js:flash.savedJob.id)}'));

                    </g:javascript>
                </g:if>

                <span id="busy" style="display:none"></span>
<g:timerEnd key="head"/>
                <g:if test="${ jobgroups}">
                    <g:timerStart key="groupTree"/>
                    <g:form controller="scheduledExecution" action="deleteBulk" useToken="true" params="[project: params.project ?: request.project]">
                    <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_DELETE  }" project="${params.project ?: request.project}">
                        <div class="modal fade" id="bulk_del_confirm" tabindex="-1" role="dialog" aria-hidden="true">
                            <div class="modal-dialog">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <button type="button" class="close" data-dismiss="modal"
                                                aria-hidden="true">&times;</button>
                                        <h4 class="modal-title">Confirm Bulk Job Delete</h4>
                                    </div>

                                    <div class="modal-body">
                                        <p><g:message code="really.delete.these.jobs"/></p>
                                    </div>

                                    <div class="modal-footer">
                                        <button type="button"
                                                class="confirm_decline btn btn-default"
                                                data-dismiss="modal" ><g:message code="no"/></button>
                                        <g:submitButton name="${g.message(code: 'yes')}" class="btn btn-danger"/>
                                    </div>
                                </div><!-- /.modal-content -->
                            </div><!-- /.modal-dialog -->
                        </div><!-- /.modal -->
                    <div class="floatr" style="margin-top: 10px;">
                        <div>
                            <span class="btn btn-warning btn-xs job_bulk_edit bulk_edit_invoke"><g:message code="bulk.delete" /></span>
                        </div>
                        <div class="bulk_edit_controls panel panel-warning" style="display: none">
                            <div class="bulk_edit_controls panel-heading">
                                <button type="button" class="close job_bulk_edit_hide "
                                        aria-hidden="true">&times;</button>
                                <h3 class="panel-title"><g:message code="select.jobs.to.delete"/>

                                </h3>
                            </div>
                            <div class="panel-body">
                                <span class="btn btn-default btn-xs job_bulk_select_none" ><g:message code="select.none" /></span>
                                <span class="btn btn-default btn-xs job_bulk_select_all" ><g:message code="select.all" /></span>

                            </div>

                            <div class="panel-footer">
                                <a id="bulk_del_prompt"
                                      data-toggle="modal"
                                      href="#bulk_del_confirm"
                                      class="btn btn-warning btn-sm" ><g:message code="delete.selected.jobs" /></a>
                            </div>

                        </div>
                    </div>
                    </auth:resourceAllowed>
                    <g:render template="groupTree" model="${[small:params.compact?true:false,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false,nowrunning:nowrunning, clusterMap: clusterMap,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
                    </g:form>
                    <g:timerEnd key="groupTree"/>
                </g:if>
                <g:else>
                    <div class="presentation">

                        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}" project="${params.project ?: request.project}">
                            <ul>
                            <li style="padding:5px"><g:link controller="scheduledExecution" action="create"
                                                            params="[project: params.project ?: request.project]"
                                                            class="btn btn-default btn-sm">Create a new Job&hellip;</g:link></li>
                            <li style="padding:5px"><g:link controller="scheduledExecution" action="upload"
                                                            params="[project: params.project ?: request.project]"
                                                            class="btn btn-default btn-sm">Upload a Job definition&hellip;</g:link></li>
                            </ul>
                        </auth:resourceAllowed>

                    </div>
                </g:else>
    <g:timerStart key="tail"/>
            </td>
        </tr>
    </table>
</div>

<%-- template load script, adds behavior to radio buttons to hide appropriate form elements when selected --%>
<g:javascript>
    function _set_adhoc_filters(e){
        if($F(e.target)=='true'){
            $('${enc(js:rkey)}adhocFilters').show();
            $('${enc(js:rkey)}definedFilters').hide();
        }else if($F(e.target)=='false'){
            $('${enc(js:rkey)}adhocFilters').hide();
            $('${enc(js:rkey)}definedFilters').show();
        }else{
            $('${enc(js: rkey)}adhocFilters').hide();
            $('${enc(js: rkey)}definedFilters').hide();
        }
    }
    $$('#adhocFilterPick_${enc(js: rkey)} input').each(function(elem){
        Event.observe(elem,'click',function(e){_set_adhoc_filters(e)});
    });
    $$('#${enc(js: rkey)}wffilterform input').each(function(elem){
        if(elem.type=='text'){
            elem.observe('keypress',noenter);
        }
    });
    $$('.confirm_action').each(function(elem){
        var el=$(elem.getAttribute('data-confirm'));
        Event.observe(elem,'click',function(e){
            new MenuController().showRelativeTo(elem,el);
//            $(elem).hide();
        });
    });
    $$('.confirm_decline').each(function(elem){
        var el=$(elem.getAttribute('data-confirm'));
        var view=$(elem.getAttribute('data-confirm-view'));
        Event.observe(elem,'click',function(e){
            $(el).show();
            $(view).hide();
            e.preventDefault();
        });
    });
    $$('.obs_expand_all').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.expandComponent').each(Element.show);
        });
    });
    $$('.obs_collapse_all').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.topgroup .expandComponent').each(Element.hide);
        });
    });
    $$('.job_bulk_edit').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.jobbulkeditfield').each(Element.show);
            $$('.bulk_edit_controls').each(Element.show);
            $$('.bulk_edit_invoke').each(Element.hide);
            $$('.expandComponent').each(Element.show);
        });
    });
    $$('.job_bulk_edit_hide').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.jobbulkeditfield').each(Element.hide);
            $$('.bulk_edit_controls').each(Element.hide);
            $$('.bulk_edit_invoke').each(Element.show);
            $$('.jobbulkeditfield').each(function(z){
                z.select('input[type=checkbox]').each(function(box){
                    box.checked=false;
                });
            });
        });
    });
    $$('.job_bulk_select_all').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.expandComponent').each(Element.show);
            $$('.jobbulkeditfield').each(function(z){
                z.select('input[type=checkbox]').each(function(box){
                    box.checked=true;
                });
            });
        });
    });
    $$('.job_bulk_select_none').each(function(elem){
        Event.observe(elem,'click',function(e){
            $$('.expandComponent').each(Element.show);
            $$('.jobbulkeditfield').each(function(z){
                z.select('input[type=checkbox]').each(function(box){
                    box.checked=false;
                });
            });
        });
    });

</g:javascript>
<g:timerEnd key="tail"/>
<g:timerEnd key="_workflowsFull.gsp"/>
