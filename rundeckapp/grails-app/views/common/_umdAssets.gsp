<%@ page import="org.rundeck.app.gui.UmdModule" %>
<!--UMD Modules-->
<g:set var="umdModules" value="${applicationContext.getBeansOfType(org.rundeck.app.gui.UmdModule).values()}"/>
<g:each var="m" in="${umdModules}">
    <asset:javascript src="${m.assetPath}" />
    <g:if test="${m.cssAssetPath}">
        <asset:stylesheet href="${m.cssAssetPath}" />
    </g:if>
</g:each>
<!--End UMD Modules-->