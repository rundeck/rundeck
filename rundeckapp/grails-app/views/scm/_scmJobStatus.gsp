<g:if test="${status == 'CLEAN'}">
    <span class="has_tooltip"
        data-placement="right"
          title="SCM Status: OK. [${meta?.commitId6 ?: meta?.commitId} by ${meta.authorName?:''}&lt;${meta.authorEmail?:''}&gt;]">
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : '',
                                                     meta  : meta]"/>
    </span>
</g:if>
<g:else>

    <g:link action="commit" controller="scm"
            data-placement="right"
            class="btn btn-link btn-sm has_tooltip"
            title="Click to commit or add this Job"
            params="${[project: params.project, jobIds: jobid]}">
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : text,
                                                     meta  : meta]"/>
    </g:link>
</g:else>