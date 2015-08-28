<g:set var="tooltip" value=""/>
<g:if test="${status=='CLEAN'}">
    <g:set var="tooltip" value="${message(code:'scm.export.status.clean.description',args:[meta?.commitId6 ?: meta?.commitId, meta.authorName?:'', meta.authorEmail?:''])}"/>
</g:if>
<g:else>
    <g:set var="tooltip" value="${message(
            code: 'scm.export.status.' + status + '.description',
            args: [meta?.commitId6 ?: meta?.commitId, meta.authorName ?: '', meta.authorEmail ?: ''],
            default: 'Export Needed '+status
    )}"/>
</g:else>

<span class="has_tooltip" data-placement="right" title="${tooltip}">
    <g:render template="/scm/statusIcon"
              model="[status: status,
                      text  : text,
                      icon:icon,
                      notext: notext,
                      meta  : meta]"/>
</span>
