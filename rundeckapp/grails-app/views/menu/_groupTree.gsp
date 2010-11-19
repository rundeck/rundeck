<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jul 9, 2008
  Time: 3:08:34 PM
  To change this template use File | Settings | File Templates.
--%>
<g:timerStart key="gtx"/>
<div class="jobGroups ${subtree?' subdirs':''} expandComponent" ${subtree && !expanded && !(wasfiltered) ?'style="display:none"':''}>
<g:if test="${!prefix && wasfiltered && paginateParams.groupPath}">
    <div>
        <a class=" groupname" href="${createLink(controller:'menu',action:'jobs')}" title="Top level"><img src="${resource(dir:'images',file:'icon-small-folder-up.png')}" width="16px" height="15px" alt="dir"/> Top</a>
        <g:if test="${paginateParams.groupPath.indexOf('/')>0}">
            <g:set var="uplevel" value="${paginateParams.groupPath.substring(0,paginateParams.groupPath.lastIndexOf('/'))}"/>
            <g:set var="newparams" value="${paginateParams}"/>
            %{
                newparams['groupPath']=uplevel
            }%
            <a class=" groupname" href="${createLink(controller:'menu',action:'jobs',params:newparams)}" title="Previous level"><img src="${resource(dir:'images',file:'icon-small-folder-up.png')}" width="16px" height="15px" alt="dir"/> Up</a>
        </g:if>
    </div>
</g:if>
<g:set var="gkeys" value="${jobgroups.sort{a,b->a.key<=>b.key}}"/>
<g:timerEnd key="gtx"/>
<g:set var="indentpx" value="${16}"/>
<g:set var="prevkey" value="${null}"/>
<g:set var="indent" value="${0}"/>

<g:each in="${gkeys}" var="group">
    <g:timerStart key="_groupTree2.gsp-loop"/>
    <g:timerStart key="prepare"/>
    <g:set var="displaygroup" value="${group.key}"/>
    <g:if test="${null!=prevkey && group.key.startsWith(prevkey)}">
        %{
            indent++
        }%
        <g:set var="displaygroup" value="${group.key.substring(prevkey.length()+1)}"/>
    </g:if>
    <g:else>
        <g:set var="indent" value="${0}"/>
    </g:else>
    <g:set var="prevkey" value="${group.key}"/>

    <div class="expandComponentHolder" style="${indent>0? 'margin-left: '+(indent*indentpx)+'px;':''}">
        <g:if test="${jobgroups[group.key]}">
            <g:expander open="${group.value?.size()==0||wasfiltered?'true':'false'}"/><!--
        --></g:if>
        <g:else><!--
            --><img src="${resource(dir:'images',file:'blank.gif')}" width="16px" height="12px"/><!--
        --></g:else><!--
           --><span class="expandComponentControl textbtn action groupname" onclick="${jscallback?jscallback+'(\''+(prefix?prefix+'/'+group.key:group.key)+'\');return false;':'Expander.toggle(this)'}" title="${jscallback?'Select this group':'Expand/Collapse this group'}"><!--
            --><img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt="dir"/><!--
            --><g:if test="${jscallback || jobsjscallback}">
                ${displaygroup}
            </g:if><!--
        --></span>
        <g:if test="${!jscallback && !jobsjscallback}">
            <a class=" groupname" href="${createLink(controller:'menu',action:'jobs',params:[groupPath:prefix?prefix+'/'+group.key:group.key])}">${displaygroup}</a>
        </g:if>
        <g:if test="${group.value?.size()>0}">
            (${group.value.size()})
        </g:if>
        <g:timerEnd key="prepare"/>
        <g:if test="${jobgroups[group.key]}">
            <div class="jobGroups subdirs expandComponent" style="margin-left: ${(indentpx-2)}px; ${wdgt.styleVisible(unless:group.value?.size()>0 && !(wasfiltered) )}" >
            <g:render template="jobslist" model="[jobslist:jobgroups[group.key],total:jobgroups[group.key]?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback]"/>
            </div>
        </g:if>
    </div>
    <g:timerEnd key="_groupTree2.gsp-loop"/>
</g:each>


    <g:if test="${currentJobs}">
        <g:timerStart key="_groupTree2.gsp-jobslist"/>
        <div>
        <g:render template="jobslist" model="[jobslist:currentJobs,total:currentJobs?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,jobauthorizations:jobauthorizations,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback]"/>
        </div>
        <g:timerEnd key="_groupTree2.gsp-jobslist"/>
    </g:if>
</div>