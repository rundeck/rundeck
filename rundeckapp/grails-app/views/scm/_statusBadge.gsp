<g:set var="tooltip" value=""/>
<g:if test="${status=='CLEAN'}">
    <g:set var="tooltip" value="${message(code:'scm.export.status.clean.description')}"/>
</g:if>
<g:else>
    <g:set var="tooltip" value="${message(
            code: 'scm.export.status.' + status + '.description',
            default: 'Export Needed '+status
    )}"/>
</g:else>

<span title="${tooltip}" class="has_tooltip">
    <g:if test="${link}">
        <g:link controller="scm"
                title="${g.message(
                        code: status == 'CLEAN' ? 'scm.action.diff.clean.button.label' :
                                'scm.action.diff.button.label'
                )}"
                params="[project: job.project, jobId: job.extid]"
                action="diff">

            <g:render template="/scm/statusIcon"
                      model="[status: status,
                              text  : text,
                              icon  : icon,
                              notext: notext,
                              commit: commit]"/>
        </g:link>
    </g:if>
    <g:else>
        <g:render template="/scm/statusIcon"
                  model="[status: status,
                          text  : text,
                          icon  : icon,
                          notext: notext,
                          commit: commit]"/>
    </g:else>
</span>