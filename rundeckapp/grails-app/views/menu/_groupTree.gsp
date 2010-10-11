<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jul 9, 2008
  Time: 3:08:34 PM
  To change this template use File | Settings | File Templates.
--%>

<ul class="jobGroups ${subtree?' subdirs':''} expandComponent" ${subtree && !expanded && !(wasfiltered) ?'style="display:none"':''}>
<g:if test="${!prefix && wasfiltered && paginateParams.groupPath}">
    <li><a class=" groupname" href="${createLink(controller:'menu',action:'jobs')}" title="Top level"><img src="${resource(dir:'images',file:'icon-small-folder-up.png')}" width="16px" height="15px" alt="dir"/> Top</a></li>
</g:if>
<g:each in="${groupTree.sort{a,b->a.key<=>b.key}}" var="group">
    <li class="expandComponentHolder">
        <g:if test="${group.value.subs || group.value.jobs}">
            <g:expander open="${group.value.count==0||wasfiltered?'true':'false'}" />
        </g:if>
        <g:else>
            <img src="${resource(dir:'images',file:'blank.gif')}" width="12px" height="12px"/>
        </g:else>

        <span class="expandComponentControl textbtn action groupname" onclick="${jscallback?jscallback+'(\''+(prefix?prefix+'/'+group.key:group.key)+'\');return false;':'Expander.toggle(this)'}" title="${jscallback?'Select this group':'Expand/Collapse this group'}">
            <img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt="dir"/><!--
            --><g:if test="${jscallback || jobsjscallback}">
                ${group.key}
            </g:if><!--
        --></span>
        <g:if test="${!jscallback && !jobsjscallback}">
            <a class=" groupname" href="${createLink(controller:'menu',action:'jobs',params:[groupPath:prefix?prefix+'/'+group.key:group.key])}">${group.key}</a>
        </g:if>
        <g:if test="${group.value.total>0}">
            (${group.value.total})
        </g:if>
        <g:if test="${group.value.subs || group.value.jobs}">
            <g:render controller="menu" template="groupTree" model="${[groupTree:group.value.subs,currentJobs:group.value.jobs?group.value.jobs:[],subtree:true, prefix:(prefix?prefix+'/'+group.key:group.key),expanded:group.value.count==0||wasfiltered,wasfiltered:wasfiltered,jscallback:jscallback,small:small?true:false]}"/>
        </g:if>
    </li>
</g:each>

    <g:if test="${currentJobs}">
        <li>
        <g:render template="jobslist" model="[jobslist:currentJobs,total:currentJobs?.size(),nowrunning:nowrunning,nextExecutions:nextExecutions,authMap:authMap,nowrunningtotal:nowrunningtotal,max:max,offset:offset,paginateParams:paginateParams,sortEnabled:true,headers:false,wasfiltered:wasfiltered,small:small?true:false,jobsjscallback:jobsjscallback]"/>
        </li>
    </g:if>
</ul>