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
            <g:link controller="menu" action="jobs" class="groupname" title="Previous level" params="${newparams}">
                <g:img file="icon-small-folder-up.png" width="16px" height="15px"/>
                Up
            </g:link>
        </g:if>
        <g:else>
            <g:link controller="menu" action="jobs" class="groupname" title="Top level">
                <g:img file="icon-small-folder-up.png" width="16px" height="15px"/>
                Top
            </g:link>
        </g:else>
    </div>
</g:if>
<g:set var="gkeys" value="${jobgroups.sort{a,b->a.key<=>b.key}.grep{it.key!=''}}"/>
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
        ${outdiv}

    </g:elseif>
    <g:else>
        ${(['</div>'] * divcounts).join('<!--rend-->')}
        <g:set var="level" value="${[]}"/>
        <g:set var="indent" value="${0}"/>
        <g:set var="divcounts" value="${0}"/>
        %{
            level<<group.key
        }%
    </g:else>
    <g:set var="prevkey" value="${group.key}"/>
    <g:set var="groupopen" value="${(wasfiltered || jscallback)}"/>
    ${"<"}div class="expandComponentHolder ${groupopen ? 'expanded' : ''} " ${">"}
        %{divcounts++;}%
        <div style="margin-bottom:4px;">
        <g:if test="${jscallback}">
            <span class="expandComponentControl textbtn action groupname jobgroupexpand" onclick="${jscallback + '(\'' + (prefix ? prefix + '/' + group.key : group.key) + '\');return false;' }" title="${jscallback ? 'Select this group' : 'Expand/Collapse this group'}" style="padding-left:4px;"><%--
            --%><g:img file="icon-small-folder-open.png" width="16px" height="14px"/> ${displaygroup}<%--
        --%></span>
        </g:if>
        <g:else>
            <g:set var="jsfunc" value="Expander.toggle(this,null,'.expandComponentHolder.sub_${currkey}_group');"/>
            <g:expander open="${groupopen?'true':'false'}" jsfunc="${jsfunc}" imgfirst="true" style="padding-left:4px;" classnames="jobgroupexpand">
                <span class="foldertoggle">&nbsp;</span>
            </g:expander>
            <a class=" groupname" href="${createLink(controller: 'menu', action: 'jobs', params: [groupPath: prefix ? prefix + '/' + group.key : group.key])}">${displaygroup}</a>
        </g:else>
        </div>
        
        <g:timerEnd key="prepare"/>
        ${"<"}div class="expandComponent sub_${currkey}_group sub_group" style="${wdgt.styleVisible(if: groupopen)}"${">"}
        %{ divcounts++;}%
        <g:if test="${jobgroups[group.key]}">
            <div class="jobGroups subjobs">
            <g:render template="jobslist" model="[jobslist:jobgroups[group.key],total:jobgroups[group.key]?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
            </div>
        </g:if>

    <g:timerEnd key="_groupTree2.gsp-loop"/>
</g:each>
    ${(['</div>'] * divcounts).join('<!--rlast-->')}

    <g:if test="${currentJobs}">
        <g:timerStart key="_groupTree2.gsp-jobslist"/>
        <div>
        <g:render template="jobslist" model="[jobslist:currentJobs,total:currentJobs?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback,runAuthRequired:runAuthRequired]"/>
        </div>
        <g:timerEnd key="_groupTree2.gsp-jobslist"/>
    </g:if>
</div>