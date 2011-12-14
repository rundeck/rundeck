<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
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

<div id="${rkey}wffilterform">
        <g:if test="${flash.message}">
            ${flash.message}
        </g:if>
        <g:if test="${flash.error}">
            <span class="error note">${flash.error}</span>
        </g:if>
    <g:set var="wasfiltered" value="${paginateParams.keySet().grep(~/(?!proj).*Filter|groupPath|idlist$/)}"/>
    <g:if test="${params.createFilters}">
        <span class="note help">
            Enter filter parameters below and click "save this filter" to set a name and save it.
        </span>
    </g:if>
    <g:set var="filtersOpen" value="${params.createFilters||params.editFilters||params.saveFilter?true:false}"/>
    <table cellspacing="0" cellpadding="0" width="100%">
        <tr>

            <td style="text-align:left;vertical-align:top;width:200px; ${wdgt.styleVisible(if:filtersOpen)}" id="${rkey}filter" class="wffilter" >
            <g:form action="jobs" method="get">
                <g:if test="${params.compact}">
                    <g:hiddenField name="compact" value="${params.compact}"/>
                </g:if>
                <span class="prompt action obs_filtertoggle">
                    Filter
                    <img src="${resource(dir:'images',file:'icon-tiny-disclosure-open.png')}" width="12px" height="12px"/>
                </span>
                <g:render template="/common/queryFilterManager" model="${[rkey:rkey,filterName:filterName,filterset:filterset,update:rkey+'wffilterform',deleteActionSubmit:'deleteJobfilter',storeActionSubmit:'storeJobfilter']}"/>
                
                <div class="filter">

                            <g:hiddenField name="max" value="${max}"/>
                            <g:hiddenField name="offset" value="${offset}"/>
                        <table class="simpleForm">
                            <g:if test="${params.idlist}">
                                <tr>
                                    <td><label for="${rkey}idlist"><g:message code="jobquery.title.idlist"/></label>:</td>
                                    <td><g:textField name="idlist" id="${rkey}idlist" value="${params.idlist}" /></td>
                                </tr>
                            </g:if>
                            <tr>
                                <td><label for="${rkey}jobFilter"><g:message code="jobquery.title.jobFilter"/></label>:</td>
                                <td><g:textField name="jobFilter" id="${rkey}jobFilter" value="${params.jobFilter}" /></td>
                            </tr>
                            <tr>
                                <td><label for="${rkey}groupPath"><g:message code="jobquery.title.groupPath"/></label>:</td>
                                <td><g:textField name="groupPath" id="${rkey}groupPath" value="${params.groupPath}"/></td>
                            </tr>
                            <tr>
                                <td><label for="${rkey}descFilter"><g:message code="jobquery.title.descFilter"/></label>:</td>
                                <td><g:textField name="descFilter" id="${rkey}descFilter" value="${params.descFilter}"/></td>
                            </tr>

                            <tr>
                                <td colspan="2">
                                    <g:actionSubmit  value="Filter" name="filterAll" controller='menu' action='workflows'  />
                                    <g:actionSubmit  value="Clear" name="clearFilter" controller='menu' action='workflows' />
                                </td>
                            </tr>
                            </table>
                </div>
            </g:form>

            </td>
            <td style="text-align:left;vertical-align:top;" id="${rkey}wfcontent" class="wfcontent">

                <div class="jobscontent head">
    <g:if test="${!params.compact}">
        <auth:resourceAllowed kind="job" action="${AuthConstants.ACTION_CREATE}">
        <div class=" floatr" >
            <g:link controller="scheduledExecution" action="create" class="button ">New <g:message code="domain.ScheduledExecution.title"/>&hellip;</g:link>
        </div>
        </auth:resourceAllowed>
    </g:if>

                <g:if test="${wasfiltered}">
                    <div>
                    <g:if test="${!params.compact}">
                        <span class="prompt">${total} <g:message code="domain.ScheduledExecution.title"/>s</span>
                            matching filter:
                    </g:if>

                    <g:if test="${filterset}">
                        <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        <!--<span class="info note">Filter:</span>-->
                    </g:if>
                    <g:if test="${!filterName}">
                        <span class="prompt action obs_filtersave" id="outsidefiltersave" title="Click to save this filter with a name">
                            save this filter&hellip;
                        </span>
                    </g:if></div>

                            <span title="Click to modify filter" class="info textbtn query action obs_filtertoggle"  id='${rkey}filter-toggle'>
                                <g:each in="${wasfiltered.sort()}" var="qparam">
                                    <span class="querykey"><g:message code="jobquery.title.${qparam}"/></span>:

                                    <g:if test="${paginateParams[qparam] instanceof java.util.Date}">
                                        <span class="queryvalue date" title="${paginateParams[qparam].toString().encodeAsHTML()}">
                                            <g:relativeDate atDate="${paginateParams[qparam]}"/>
                                        </span>
                                    </g:if>
                                    <g:else>
                                        <span class="queryvalue text">
                                            ${g.message(code:'jobquery.title.'+qparam+'.label.'+paginateParams[qparam].toString(),default:paginateParams[qparam].toString().encodeAsHTML())}
                                        </span>
                                    </g:else>

                                </g:each>
                                <img src="${resource(dir:'images',file:'icon-tiny-disclosure.png')}" width="12px" height="12px"/>
                            </span>
                </g:if>
                <g:else>
                    <g:if test="${!params.compact}">
                    <span class="prompt"><g:message code="domain.ScheduledExecution.title"/>s (${total})</span>
                    </g:if>

                    <span class="prompt action obs_filtertoggle"  id="${rkey}filter-toggle">
                        Filter
                        <img src="${resource(dir:'images',file:'icon-tiny-disclosure'+(wasfiltered?'-open':'')+'.png')}"  width="12px" height="12px"/></span>
                    <g:if test="${filterset}">
                        <span style="margin-left:10px;">
                            <span class="info note">Choose a Filter:</span>
                            <g:render template="/common/selectFilter" model="[noSelection:'-All-',filterset:filterset,filterName:filterName,prefName:'workflows']"/>
                        </span>
                    </g:if>
                </g:else>
                    <div class="clear"></div>
                </div>

                <g:if test="${flash.savedJob}">
                    <div class="newjob">
                    <span class="popout message note" style="background:white">
                        ${flash.savedJobMessage?flash.savedJobMessage:'Saved changes to Job'}:
                        <g:link controller="scheduledExecution" action="show" id="${flash.savedJob.id}">${flash.savedJob.generateFullName().encodeAsHTML()}</g:link>
                    </span>
                    </div>
                    <g:javascript>
                        fireWhenReady('jobrow_${flash.savedJob.id}',doyft.curry('jobrow_${flash.savedJob.id}'));

                    </g:javascript>
                </g:if>

                <span id="busy" style="display:none"></span>
<g:timerEnd key="head"/>
                <g:if test="${ jobgroups}">
                    <g:timerStart key="groupTree"/>
                    <g:render template="groupTree" model="${[small:params.compact?true:false,currentJobs:jobgroups['']?jobgroups['']:[],wasfiltered:wasfiltered?true:false,nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true]}"/>
                    <g:timerEnd key="groupTree"/>
                </g:if>
    <g:timerStart key="tail"/>
            </td>
        </tr>
    </table>
</div>

<%-- template load script, adds behavior to radio buttons to hide appropriate form elements when selected --%>
<g:javascript>
    function _set_adhoc_filters(e){
        if($F(e.target)=='true'){
            $('${rkey}adhocFilters').show();
            $('${rkey}definedFilters').hide();
        }else if($F(e.target)=='false'){
            $('${rkey}adhocFilters').hide();
            $('${rkey}definedFilters').show();
        }else{
            $('${rkey}adhocFilters').hide();
            $('${rkey}definedFilters').hide();
        }
    }
    $$('#adhocFilterPick_${rkey} input').each(function(elem){
        Event.observe(elem,'click',function(e){_set_adhoc_filters(e)});
    });
    $$('#${rkey}wffilterform input').each(function(elem){
        if(elem.type=='text'){
            elem.observe('keypress',noenter);
        }
    });

</g:javascript>
<g:timerEnd key="tail"/>
<g:timerEnd key="_workflowsFull.gsp"/>
