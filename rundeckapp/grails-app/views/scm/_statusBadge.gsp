<g:if test="${showClean && (exportStatus || importStatus) ||
        exportStatus && exportStatus?.toString() != 'CLEAN' ||
        importStatus && importStatus?.toString() != 'CLEAN'}">
    <g:set var="tooltips" value="${[]}"/>
    <g:if test="${exportStatus}">
        %{
            tooltips << message(
                    code: "scm.export.status.${exportStatus}.description",
                    default: exportStatus.toString()
            )
        }%
    </g:if>
    <g:if test="${importStatus}">
        %{
            tooltips << message(
                    code: "scm.import.status.${importStatus}.description",
                    default: importStatus.toString()
            )
        }%
    </g:if>
    <span title="${tooltips.join(", ")}" class="has_tooltip">
        <g:render template="/scm/statusIcon"
                  model="[exportStatus: exportStatus,
                          importStatus: importStatus,
                          showClean   : showClean,
                          text        : text,
                          icon        : icon,
                          notext      : notext,
                          exportCommit: exportCommit,
                          importCommit: importCommit]"/>
    </span>

</g:if>
