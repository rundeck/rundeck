<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: Aug 8, 2008
  Time: 5:15:16 PM
  To change this template use File | Settings | File Templates.
--%>
<g:each in="${displayParams.keySet().sort()}" var="qparam">
    <span class="querykey"><g:message code="jobquery.title.${qparam}"/></span>:

    <g:if test="${displayParams[qparam] instanceof java.util.Date}">
        <span class="queryvalue date" title="${displayParams[qparam].toString().encodeAsHTML()}">
            <g:relativeDate atDate="${displayParams[qparam]}"/>
        </span>
    </g:if>
    <g:elseif test="${qparam=='recentFilter' && '-'!=displayParams[qparam]}">
        <span class="queryvalue date">
            <g:recentDescription value="${displayParams[qparam]}"/>
        </span>
    </g:elseif>
    <g:elseif test="${qparam=='maprefUriFilter'}">
        <span class="queryvalue text" title="${displayParams[qparam]}">
            &lt;&hellip;${displayParams[qparam].lastIndexOf("#")>=0?displayParams[qparam].substring(displayParams[qparam].lastIndexOf("#")):''}&gt;
        </span>
    </g:elseif>
    <g:else>
        <span class="queryvalue text">
            ${displayParams[qparam]}
        </span>
    </g:else>

</g:each>