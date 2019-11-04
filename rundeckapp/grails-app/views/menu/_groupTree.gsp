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

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jul 9, 2008
  Time: 3:08:34 PM
  To change this template use File | Settings | File Templates.
--%>
<%-- //TODO review timerEnd for removal from this template --%>
<g:timerStart key="gtx"/>
<div class="job-display-tree jobGroups ${subtree?' subdirs':'topgroup'} expandComponent" ${subtree && !expanded && !(wasfiltered) ?'style="display:none"':''}>

<g:set var="gkeys" value="${g.sortGroupKeys(groups: jobgroups.grep {it.key != ''})}"/>
<g:timerEnd key="gtx"/>
<g:set var="prevkey" value="${null}"/>
<g:set var="indent" value="${0}"/>
<g:set var="divcounts" value="${0}"/>
<g:set var="level" value="${[]}"/>

<g:each in="${gkeys}" var="group">
    <g:timerStart key="_groupTree2.gsp-loop"/>
    <g:timerStart key="prepare"/>
    <g:set var="displaygroup" value="${group.key}"/>
    <g:set var="currkey" value="${g.rkey()}"/>
    <g:if test="${prevkey && group.key.startsWith(prevkey+'/')}">
        %{
            indent++;
            level<<group.key
        }%
        <g:set var="displaygroup" value="${group.key.substring(prevkey.length()+1)}"/>
    </g:if>
    <g:elseif test="${level && level.findLastIndexOf {group.key.startsWith(it+'/')}>=0}">
        <g:set var="found" value="${level.findLastIndexOf{group.key.startsWith(it+'/')}}"/>
        <g:set var="count" value="${level.size()-(found+1)}"/>
        <g:set var="displaygroup" value="${group.key.substring(level[found].length()+1)}"/>
        <g:set var="outdiv" value=""/>
        %{
            indent-=count;
            level = level[0..found];
            level<<group.key;
            outdiv= (['</div>'] * (count*2)).join('<!-- x -->');
            divcounts-=(count*2);
         }%
        ${raw(outdiv)}

    </g:elseif>
    <g:else>
        ${raw((['</div>'] * divcounts).join('<!--rend-->'))}
        <g:set var="level" value="${[]}"/>
        <g:set var="indent" value="${0}"/>
        <g:set var="divcounts" value="${0}"/>
        %{
            level<<group.key
        }%
    </g:else>
    <g:set var="prevkey" value="${group.key}"/>
    <g:set var="groupopen" value="${(wasfiltered || jscallback || (level.size()<= jobExpandLevel || jobExpandLevel<0))}"/>
    ${raw("<")}div class="expandComponentHolder  ${groupopen ? 'expanded' : ''} " style="" ${raw(">")}
        %{divcounts++;}%
        <div class="job_list_group_header hover-reveal-hidden">
        <g:if test="${jscallback}">
            <span class="expandComponentControl textbtn textbtn-success groupname jobgroupexpand"
                  title="Select this group"
                  onclick="groupChosen('${enc(js:prefix ? prefix + '/' + group.key : group.key)}'); return false;"
                  >
              <i class="glyphicon glyphicon-folder-close"></i> <g:enc>${displaygroup}</g:enc>
            </span>
        </g:if>
        <g:else>
            <g:set var="jsfunc" value="Expander.toggle(this,null,'.expandComponentHolder.sub_${currkey}_group');"/>
            <g:expander open="${groupopen?'true':'false'}" jsfunc="${jsfunc}" imgfirst="true"  classnames="jobgroupexpand text-secondary autoclickable" iconCss="text-muted">

                <g:enc>${displaygroup}</g:enc>

                <g:if test="${!jobsjscallback}">
                    <a class="groupname text-primary visibility-hidden "
                    title="Browse job group: ${enc(attr:prefix ? prefix + '/' + group.key : group.key)}"
                        href="${createLink(controller: 'menu', action: 'jobs', params: [project:params.project,groupPath: prefix ? prefix + '/' + group.key : group.key])}"><i class="glyphicon glyphicon-folder-open"></i></a>
                </g:if>
            </g:expander>
            <g:if test="${!jobsjscallback}">

                <g:if test="${jobgroups[group.key]}">
                <span class="" data-bind="visible: enabled">
                    &bull;
                    <a href="#" class="btn btn-xs btn-simple btn-hover" data-job-group="${group.key}" data-bind="click: function(){jobGroupSelectAll($element);}">
                        <g:icon name="check"/>
                        <g:message code="select.all" />
                    </a>
                    <a href="#" class="btn btn-xs btn-simple btn-hover" data-job-group="${group.key}" data-bind="click: function(){jobGroupSelectNone($element);}">
                        <g:icon name="unchecked"/>
                        <g:message code="select.none" />
                    </a>
                </span>

                </g:if>
            </g:if>
        </g:else>
        </div>

        <g:timerEnd key="prepare"/>
    ${raw("<")}div class="expandComponent  sub_${currkey}_group sub_group" style="${wdgt.styleVisible(if: groupopen)}"${raw(">")}
        %{ divcounts++;}%
        <g:if test="${jobgroups[group.key]}">
            <div class="jobGroups subjobs  ">
            <g:render template="jobslist" model="[hideSummary:true,jobslist:jobgroups[group.key],total:jobgroups[group.key]?.size(), clusterMap: clusterMap,nextExecutions:nextExecutions,calendars:calendars,jobauthorizations:jobauthorizations,authMap:authMap,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
            </div>
        </g:if>

    <g:timerEnd key="_groupTree2.gsp-loop"/>
</g:each>
    ${raw((['</div>'] * divcounts).join('<!--rlast-->'))}

    <g:if test="${currentJobs}">
        <g:timerStart key="_groupTree2.gsp-jobslist"/>
        <div>
        <g:render template="jobslist" model="[jobslist:currentJobs,total:currentJobs?.size(),nextExecutions:nextExecutions,calendars:calendars,jobauthorizations:jobauthorizations,authMap:authMap,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
        </div>
        <g:timerEnd key="_groupTree2.gsp-jobslist"/>
    </g:if>
</div>
