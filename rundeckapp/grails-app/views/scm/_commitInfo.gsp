<g:if test="${title}">
    <h3>${title}</h3>
</g:if>
<g:set var="rkey" value="${g.rkey()}"/>
<blockquote>
    ${commit.message}
    <footer>
        ${commit.author}
        <g:relativeDate elapsed="${commit.date}"/>
        in
        <g:expander key="commitInfo_${rkey}" classnames="textbtn-info">
            <cite>
                ${commit.commitId}
            </cite>
        </g:expander>
    </footer>
</blockquote>

<table class="table table-bordered table-condensed table-striped "
    id="commitInfo_${rkey}"
       style="display:none">
    <g:set var="map" value="${commit.asMap()}"/>
    <g:each in="${map.keySet().sort()}" var="key">
        <tr>
            <td>${key}</td>
            <td>${map[key]}</td>
        </tr>
    </g:each>
</table>