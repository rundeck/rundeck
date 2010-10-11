<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Jul 9, 2008
  Time: 6:15:24 PM
  To change this template use File | Settings | File Templates.
--%>
<%-- Make breadcrumb links for group path --%>
<%
    def pathar = groupPath.split("/")
    def paths=[]
    for(def i=0;i<pathar.size();i++){
        paths<<[name:pathar[i],path:pathar[0..i].join("/")]
    }
%>
<img src="${resource(dir:'images',file:'icon-small-folder.png')}" width="16px" height="15px" alt=""/>
<g:each in="${paths}" var="path" status="i">
    <g:if test="${ i > 0 }">
        /
    </g:if>
    <%
        def parms = path.path?[groupPath:path.path]:[:]
        def stylecls=path.path.equals(groupPath) ?' currentdir':''
        def title = ''
        if('*'==path.path){
            title='All s'+g.message(code:'domain.ScheduledExecution.title')+'s'
            stylecls+=' alljobs'
        }else if(path.path){
            title=path.path+(groups?(' ('+(groups[path.path]?groups[path.path]:'0')+')'):'')
        }else{
            title='Top Level'
        }
    %>
    ${path.name !="*"? path.name: 'All '+g.message(code:'domain.ScheduledExecution.title')+'s'}
    <g:if test="${ i == paths.size()-1 && '*'!=groupPath}">
        ${(groups?(' ('+(groups[path.path]?groups[path.path]:'0')+')'):'')}
    </g:if>
</g:each>