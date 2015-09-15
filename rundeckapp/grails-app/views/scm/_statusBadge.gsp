
<g:if test="${exportStatus && exportStatus?.toString() != 'CLEAN' ||
              importStatus && importStatus?.toString() != 'CLEAN'}">
    <g:set var="tooltips" value="${[]}"/>
    <g:if test="${exportStatus && exportStatus?.toString()!='CLEAN'}">
        %{
            tooltips<<message(
                    code: "scm.export.status.${exportStatus}.description",
                    default: exportStatus.toString()
            )
        }%
    </g:if>
    <g:if test="${importStatus && importStatus?.toString()!='CLEAN'}">
        %{
            tooltips<<message(
                    code: "scm.import.status.${importStatus}.description",
                    default: importStatus.toString()
            )
        }%
    </g:if>
    <span title="${tooltips.join(", ")}" class="has_tooltip">
        <g:render template="/scm/statusIcon"
                  model="[exportStatus: exportStatus,
                          importStatus: importStatus,
                          text        : text,
                          icon        : icon,
                          notext      : notext,
                          exportCommit: exportCommit,
                          importCommit: importCommit]"/>
    </span>

</g:if>
