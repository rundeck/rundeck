<g:set var="tooltip" value=""/>
<g:if test="${status=='CLEAN'}">
    <g:set var="tooltip" value="${message(code:'scm.export.status.clean.description',args:[
            commit?.commitId, commit?.author ?:''])}"/>
</g:if>
<g:else>
    <g:set var="tooltip" value="${message(
            code: 'scm.export.status.' + status + '.description',
            args: [commit?.commitId, commit?.author ?: ''],
            default: 'Export Needed '+status
    )}"/>
</g:else>

<span class="has_tooltip" data-placement="right" title="${tooltip}">
    <g:render template="/scm/statusIcon"
              model="[status: status,
                      text  : text,
                      icon:icon,
                      notext: notext,
                      commit  : commit]"/>
</span>
