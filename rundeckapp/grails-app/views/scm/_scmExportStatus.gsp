<g:if test="${status.toString() == 'CLEAN'}">
    <span class="has_tooltip"
        data-placement="bottom"
          title="${message(code:'scm.export.status.CLEAN.description')}">
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : '',
                                                     integration:'export',
                                                     notext:true,
                                                     meta  : meta]"/>
    </span>
</g:if>
<g:else>
    <span class="has_${popover?'popover':'tooltip'}"
            data-placement="left"
            data-toggle="${popover?'popover':''}"
            data-content="${popover?:''}"
            data-trigger="hover"
              title="${message(code:"scm.export.status.${status}.title.text")}">
    %{--<g:link action="exportAction" controller="scm"--}%
            %{--data-placement="left"--}%
            %{--data-toggle="${popover?'popover':''}"--}%
            %{--data-content="${popover?:''}"--}%
            %{--data-trigger="hover"--}%
            %{--class="btn btn-sm btn-link has_${popover?'popover':'tooltip'}"--}%
            %{--title="${message(code:"scm.export.status.${status}.title.text")}"--}%
            %{--params="${[project: params.project, allJobs:true]}">--}%
        <g:render template="/scm/statusIcon" model="[status: status,
                                                     text  : text,
                                                     integration:'export',
                                                     meta  : meta]"/>
    </span>
    %{--</g:link>--}%
</g:else>