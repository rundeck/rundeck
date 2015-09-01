<g:if test="${status.toString() == 'CLEAN'}">
    <span class="has_tooltip"
        data-placement="bottom"
          title="${message(code:'scm.export.status.clean.description')}">
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : '',
                                                     notext:true,
                                                     meta  : meta]"/>
    </span>
</g:if>
<g:else>

    <g:link action="commit" controller="scm"
            data-placement="bottom"
            class="btn btn-sm btn-link has_tooltip"
            title="${message(code:"scm.export.commit.link.title")}"
            params="${[project: params.project, allJobs:true]}">
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : text,
                                                     meta  : meta]"/>
    </g:link>
</g:else>