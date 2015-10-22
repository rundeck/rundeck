<%-- statuses are present and not both CLEAN --%>
<g:if test="${(exportStatus && exportStatus.toString() != 'CLEAN' || importStatus && importStatus.toString() != 'CLEAN')}">
    <g:set var="messages" value="${[export:exportMessage, import:importMessage].findAll { it.value }}"/>
    <span class=""
          data-placement="left"
          data-toggle="${messages ? 'popover' : ''}"
          data-popover-content-ref="#scmStatusPopover"
          data-trigger="hover"
          title="Project Import/Export Status">

        <g:render template="/scm/statusIcon" model="[exportStatus: exportStatus,
                                                     importStatus: importStatus,
                                                     text        : text,
                                                     meta        : meta]"/>
    </span>
    <div id="scmStatusPopover" style="display: none;">
        <dl>
        <g:each in="${messages}" var="msg">
            <g:if test="${msg.value}">

                <dt><g:message code="scm.${msg.key}.title"/></dt>
                <dd>
                    ${msg.value}
                </dd>
            </g:if>
        </g:each>
        </dl>
    </div>
</g:if>