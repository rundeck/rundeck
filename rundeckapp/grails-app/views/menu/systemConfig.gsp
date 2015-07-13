<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 10/25/13
  Time: 3:50 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="com.dtolabs.rundeck.core.common.FilesystemFramework; com.dtolabs.rundeck.core.common.Framework" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="gui.menu.SystemConfig" /></title>
</head>

<body>
<div class="row">
    <div class="col-sm-12">
        <g:render template="/common/messages"/>
    </div>
</div>
<div class="row">
    <div class="col-sm-3">
        <g:render template="configNav" model="[selected: 'syscfg']"/>

    </div>

    <g:set var="flatConfig" value="${grailsApplication.config.flatten()}"/>
    <g:set var="fwkProps" value="${rundeckFramework.propertyLookup.propertiesMap}"/>
    <div class="col-sm-9">
    <div class="well well-sm">
    <div class="text-info">
        <g:message code="page.SystemConfiguration.description" />
    </div>
    </div>
    <h4>Server Connection Info</h4>
        <g:set var="fwkPropFile" value="${FilesystemFramework.getPropertyFile(rundeckFramework.getConfigDir())}"/>

        <div class="text-muted"><g:enc>${fwkPropFile.absolutePath}</g:enc>:</div>

        <g:render template="displayConfigProps" model="[obscurePattern:~/password/,map: fwkProps,
            keys: fwkProps.keySet().findAll{it=~/^framework.server/}]"/>

        <div class="text-muted"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>

        <g:render template="displayConfigProps" model="[map: flatConfig, keys: ['grails.serverURL']]"/>

    <h4>SSH Defaults</h4>

        <div class="text-muted"><g:enc>${fwkPropFile.absolutePath}</g:enc>:</div>

        <g:render template="displayConfigProps" model="[map: fwkProps,keys: fwkProps.keySet().findAll{it=~/^framework\.ssh\./}]"/>

    <h4>Datasource</h4>

        <div class="text-muted"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>

        <g:render template="displayConfigProps" model="[map: flatConfig, keys: ['dataSource.url']]"/>

    <h4>Plugins</h4>

        <div class="text-muted"><g:enc>${System.properties['rundeck.config.location']}</g:enc>:</div>


    <g:render template="displayConfigProps" model="[map: flatConfig, keys: flatConfig.keySet().grep(~/^rundeck\.execution\.logs\..*$/)]"/>

    </div>
</div>
</body>
</html>
