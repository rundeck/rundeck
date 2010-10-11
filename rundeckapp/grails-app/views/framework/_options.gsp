<g:each var="opt" in="${options.sort{a,b-> a.parameter<=> b.parameter }}">
  <g:if test="${! opt.getRequired()}">[</g:if>
  -${opt.getParameter()}
  <g:if test="${opt.isStringType()}">
    <g:set var="argString" value="&lt; ${(opt.getDefault())? opt.getDefault():''} &gt;"/>
    <span style="font-family:Courier; color: #555599;">
      ${argString}
    </span>
  </g:if>
  <g:if test="${! opt.getRequired()}">]</g:if>
</g:each>
