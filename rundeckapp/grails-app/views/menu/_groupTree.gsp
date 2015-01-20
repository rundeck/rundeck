<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jul 9, 2008
  Time: 3:08:34 PM
  To change this template use File | Settings | File Templates.
--%>
<g:timerStart key="gtx"/>
<div class="jobGroups ${subtree?' subdirs':'topgroup'} expandComponent" ${subtree && !expanded && !(wasfiltered) ?'style="display:none"':''}>
<g:if test="${!prefix && wasfiltered && paginateParams.groupPath}">
    <div style="margin-bottom:4px">
        <g:if test="${paginateParams.groupPath.indexOf('/')>0}">
            <g:set var="uplevel" value="${paginateParams.groupPath.substring(0,paginateParams.groupPath.lastIndexOf('/'))}"/>
            <g:set var="newparams" value="${paginateParams}"/>
            %{
                newparams['groupPath']=uplevel
            }%
            <g:link controller="menu" action="jobs" class="groupname" title="Previous level" params="${newparams+[project:params.project]}">
                <i class="glyphicon glyphicon-arrow-up"></i>
                Up
            </g:link>
        </g:if>
        <g:else>
            <g:link controller="menu" action="jobs" class="groupname" title="Top level" params="[project: params.project]">
                <i class="glyphicon glyphicon-arrow-up"></i>
                Top
            </g:link>
        </g:else>
    </div>
</g:if>
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
    <g:set var="groupopen" value="${(wasfiltered || jscallback || level.size()==1)}"/>
    ${raw("<")}div class="expandComponentHolder ${groupopen ? 'expanded' : ''} " ${raw(">")}
        %{divcounts++;}%
        <div style="margin-bottom:4px;">
        <g:if test="${jscallback}">
            <span class="expandComponentControl textbtn textbtn-success groupname jobgroupexpand"
                  title="Select this group"
                onclick="groupChosen('${enc(js:prefix ? prefix + '/' + group.key : group.key)}'); return false;"
                style="padding-left:4px;"><%--
            --%><i class="glyphicon glyphicon-folder-close"></i> <g:enc>${displaygroup}</g:enc><%--
        --%></span>
        </g:if>
        <g:else>
            <g:set var="jsfunc" value="Expander.toggle(this,null,'.expandComponentHolder.sub_${currkey}_group');"/>
            <g:expander open="${groupopen?'true':'false'}" jsfunc="${jsfunc}" imgfirst="true" style="padding-left:4px;" classnames="jobgroupexpand textbtn-secondary">
                <span class="foldertoggle">&nbsp;</span>
                <g:if test="${jobsjscallback}">
                    <g:enc>${displaygroup}</g:enc>
                </g:if>
            </g:expander>
            <g:if test="${!jobsjscallback}">
            <a class=" groupname secondary" href="${createLink(controller: 'menu', action: 'jobs', params: [project:params.project,groupPath: prefix ? prefix + '/' + group.key : group.key])}"><g:enc>${displaygroup}</g:enc></a>
            </g:if>
        </g:else>
        </div>

        <g:timerEnd key="prepare"/>
    ${raw("<")}div class="expandComponent sub_${currkey}_group sub_group" style="${wdgt.styleVisible(if: groupopen)}"${raw(">")}
        %{ divcounts++;}%
        <g:if test="${jobgroups[group.key]}">
            <div class="jobGroups subjobs">
            <g:render template="jobslist" model="[hideSummary:true,jobslist:jobgroups[group.key],total:jobgroups[group.key]?.size(),nowrunning:nowrunning, clusterMap: clusterMap,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
            </div>
        </g:if>

    <g:timerEnd key="_groupTree2.gsp-loop"/>
</g:each>
    ${raw((['</div>'] * divcounts).join('<!--rlast-->'))}

    <g:if test="${currentJobs}">
        <g:timerStart key="_groupTree2.gsp-jobslist"/>
        <div>
        <g:render template="jobslist" model="[jobslist:currentJobs,total:currentJobs?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
        </div>
        <g:timerEnd key="_groupTree2.gsp-jobslist"/>
    </g:if>
</div>
