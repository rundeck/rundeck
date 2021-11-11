%{--
- Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
-     http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
--}%

<%--
Created by IntelliJ IDEA.
User: greg
Date: 10/2/13
Time: 3:13 PM
To change this template use File | Settings | File Templates.
--%>

<%@ page import="java.util.regex.Pattern; com.dtolabs.rundeck.core.plugins.configuration.PropertyScope; org.rundeck.core.auth.AuthConstants" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="base"/>
  <meta name="tabpage" content="configure"/>
  <meta name="tabtitle" content="${g.message(code:'page.Plugins.title')}"/>
  <title><g:message code="page.Plugins.title"/></title>
</head>
<g:set var="authAdmin" value="${auth.resourceAllowedTest(
        type: AuthConstants.TYPE_RESOURCE,
        kind: AuthConstants.TYPE_SYSTEM,
        action: [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN],
        any: true,
        context: AuthConstants.CTX_APPLICATION
)}"/>
<body>
<div class="content">
<div id="layoutBody">
  <g:if test="${flash.errors}">
      <div class="alert alert-warning">
          <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
          <g:each in="${flash.errors}" var="message">
             <g:message code="${message}" default="${message}" />
          </g:each>
      </div>
  </g:if>
  <g:if test="${flash.installSuccess}">
      <div class="alert alert-info">
          <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
          <g:message code="plugin.install.success" default="Plugin Installed Successfully" />
      </div>
  </g:if>
  <div class="container-fluid">
    <div class="row">
      <div class="col-xs-12">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">
              <g:message code="page.Plugins.title"/>
              <g:set var="repoEnabled" value="${g.rConfig(value: "feature.repository.enabled", type: 'string') in [true,'true']}"/>

              <g:if test="${repoEnabled}">
                <div class="btn-toolbar">
                  <g:link controller="artifact" action="index" class="btn btn-success pull-right">
                    <g:message code="gui.admin.GetPlugins" default="Get Plugins"/>
                    <i class="glyphicon glyphicon-arrow-right"></i>
                  </g:link>
                  <a href="#installplugin" onclick="jQuery('#installplugin').modal('show');" class="btn-group btn btn-success pull-right">
                    <i class="glyphicon glyphicon-plus"></i>
                    <g:message code="gui.admin.InstallPlugin" default="Install Plugin"/>
                  </a>
                </div>
              </g:if>
              <g:else>
                <g:set var="pluginParams" value="${g.helpLinkParams(campaign: 'getpluginlink')}"/>
                <g:set var="pluginUrl" value="http://rundeck.org/plugins/?${pluginParams}"/>
                <g:set var="pluginLinkUrl" value="${g.rConfig(value: "gui.pluginLink", type: 'string', defValue:pluginUrl)}"/>

                <div class="btn-toolbar">
                  <a href="${enc(attr:pluginLinkUrl)}" class="btn-group btn btn-success pull-right">
                    <g:message code="gui.admin.GetPlugins" default="Get Plugins"/>
                    <i class="glyphicon glyphicon-arrow-right"></i>
                  </a>
                  <a href="#installplugin" onclick="jQuery('#installplugin').modal('show');" class="btn-group btn btn-success pull-right">
                    <i class="glyphicon glyphicon-plus"></i>
                    <g:message code="gui.admin.InstallPlugin" default="Install Plugin"/>
                  </a>
                </div>
              </g:else>
            </h3>
          </div>
          <div class="card-content">
            <div class="row">
              <div class="col-xs-12">
                <div class="text-info">
                  <g:markdown><g:message code="page.Plugins.description2.md"/></g:markdown>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-xs-12">
        <div>
          <div class="panel-group form-horizontal" id="accordion">
            <g:each in="${descriptions}" var="plugin">
            <g:set var="serviceName" value="${plugin.key}"/>
            <g:set var="pluginDescList" value="${plugin.value}"/>
            <g:set var="serviceDefaultScope"
            value="${serviceDefaultScopes && serviceDefaultScopes[serviceName] ? serviceDefaultScopes[serviceName] : PropertyScope.Project}"/>
            <g:set var="ukey" value="${g.rkey()}"/>
            <div class="panel panel-default">
              <div class="panel-heading">
                <h4 class="panel-title">
                  <a class="accordion-toggle collapsed" data-toggle="collapse" data-parent="#accordion" href="#${ukey}" title="${serviceName}">
                    <i class="auto-caret text-strong"></i>
                    <g:message code="framework.service.${serviceName}.label.plural" default="${serviceName}"/></a>

                    <g:if test="${pluginDescList.size()>0}">
                    <span class="label label-default"><g:enc>${pluginDescList.size()}</g:enc></span>
                  </g:if>
                  <g:else>
                  <small>
                    <g:enc>(${pluginDescList.size()})</g:enc>
                  </small>
                </g:else>
                <span class="text-strong"><g:message code="framework.service.${serviceName}.description" default=""/></span>
                </h4>
              </div>

              <div id="${enc(attr:ukey)}" class="panel-collapse collapse">
                <div class="panel-body">
                  <g:set var="pluginExtendedDesc" value='${message(code:"framework.service.${serviceName}.extended.description",default:"")}'/>
                  <g:if test="${pluginExtendedDesc}">
                  <span class="text-info">
                    <g:markdown>${pluginExtendedDesc}</g:markdown>
                  </span>
                </g:if>
                <div class="panel-group" id="accordion${enc(attr:ukey)}">
                  <g:each in="${pluginDescList}" var="${pluginDescription}">
                    <g:set var="pluginName" value="${pluginDescription.name}"/>
                    <g:set var="pluginTitle" value="${pluginDescription.title}"/>
                    <g:set var="pluginDesc" value="${pluginDescription.description}"/>
                    <g:set var="ukeyx" value="${g.rkey()}"/>
                    <div class="panel panel-default">
                      <div class="panel-heading">
                        <div class=" panel-title row">
                          <div class="col-sm-8">

                            <a class="accordion-toggle link-bare collapsed" data-toggle="collapse"
                            data-parent="#accordion${enc(attr: ukey)}" href="#${enc(attr: ukeyx)}" title="${pluginName}">
                              <i class="auto-caret text-strong"></i>
                              <g:set var="pluginFileMetadata" value="${uiPluginProfiles?.get(serviceName + ":" + pluginName)?.metadata}"/>
                              <stepplugin:pluginIcon service="${serviceName}"
                                name="${pluginName}"
                                width="16px"
                                height="16px">
                                <i class="rdicon icon-small plugin"></i>
                              </stepplugin:pluginIcon>
                              <stepplugin:message
                                service="${serviceName}"
                                name="${pluginName}"
                                code="plugin.title"
                                default="${pluginTitle?:pluginName}"/>
                            </a>
                            <g:if test="${pluginDesc}">
                              <g:render template="/scheduledExecution/description"
                                model="[description: stepplugin.messageText(
                                service: serviceName,
                                name: pluginName,
                                code: 'plugin.description',
                                default: pluginDesc
                                ), textCss: 'text-strong',
                                mode: 'hidden', rkey: g.rkey()]"/>
                            </g:if>
                          </div>

                          <g:set var="source" value=" " />
                          <g:set var="metaInfo" value=" " />
                          <g:set var="siteInfo" value=" " />
                          <g:set var="authorInfo" value=" " />
                            %{
                              source =  pluginFileMetadata?.filename && embeddedFilenames &&
                              embeddedFilenames.contains(pluginFileMetadata?.filename) ?
                              'embed' :

                              bundledPlugins && bundledPlugins[serviceName] &&
                              bundledPlugins[serviceName].contains(pluginName) ?
                              'builtin' :
                              'file';

                              metaInfo= (
                              pluginFileMetadata?.pluginDate
                              ? message( code: 'plugin.metadata.date', args: [pluginFileMetadata?.pluginDate] )
                              : ''
                              ) + '\n' + message ( code : 'plugin.source.' + source );

                              siteInfo = (pluginFileMetadata?.pluginUrl ? message(code:"plugin.metadata.website", args:[pluginFileMetadata?.pluginUrl]) : '');
                              authorInfo=(pluginFileMetadata?.pluginAuthor ? message(code:"plugin.metadata.author", args:[pluginFileMetadata?.pluginAuthor]) : '');
                            }%
                          <g:set var="linkInfo" value="${siteInfo} ${authorInfo}"/>
                          <div class="col-sm-4 text-right">
                            <g:if test="${pluginFileMetadata?.pluginUrl}">
                              <a class="textbtn small textbtn-info has_tooltip " title="${enc(attr: linkInfo)}" href="${g.enc(attr: pluginFileMetadata?.pluginUrl)}">
                                ${pluginFileMetadata?.pluginAuthor ?: 'site'}
                              </a> &nbsp;
                            </g:if>
                            <g:elseif test="${pluginFileMetadata?.pluginAuthor}">
                              <span class="text-strong small has_tooltip" title="${message(code:"author",encodeAs:'HTMLAttribute')}">${pluginFileMetadata?.pluginAuthor}</span>
                            </g:elseif>

                            <span class=" ${source == 'file' ? 'label label-info' : 'text-strong small'} has_tooltip " data-container="body" title="${enc(attr: metaInfo)}">${pluginFileMetadata?.pluginFileVersion ?: ''}
                              <g:if test="${source == 'embed'}">
                                <g:icon name="file"/>
                              </g:if>
                              <g:elseif test="${source == 'builtin'}">
                                <g:icon name="briefcase"/>
                              </g:elseif>
                              <g:else>
                                <g:icon name="file"/>
                              </g:else>
                            </span>
                          </div>
                        </div>
                      </div>

                      <div id="${enc(attr:ukeyx)}" class="panel-collapse collapse">
                        <div class="panel-body">
                          <div><g:message code="provider.name" />: <code>${pluginName}</code></div>
                          <g:render template="/scheduledExecution/description"
                            model="[description: stepplugin.messageText(
                            service: serviceName,
                            name: pluginName,
                            code: 'plugin.description',
                            default: pluginDesc
                            ), textCss: 'text-strong',
                            mode: 'shown', rkey: g.rkey()]"/>
                          <g:if test="${specialConfiguration[serviceName]}">
                            <div class="text-info">
                              <g:markdown>${specialConfiguration[serviceName].description.replaceAll(Pattern.quote('${pluginName}'), pluginName)}</g:markdown>
                            </div>
                          </g:if>
                          <g:each in="${pluginDescription?.properties}" var="prop">
                            <g:set var="outofscope" value="${prop.scope && !prop.scope.isInstanceLevel() && !prop.scope.isUnspecified()}"/>
                            <g:render
                              template="/framework/pluginConfigPropertyFormField"
                              model="${[service:serviceName,
                                provider:pluginName,
                                prop: prop,
                                prefix: specialConfiguration[serviceName]?.prefix?:'',
                                specialConfiguration: specialConfiguration[serviceName],
                                error: null,
                                values: [:],
                                fieldname: prop.name,
                                origfieldname: 'orig.' + prefix + prop.name,
                                outofscope: true,
                                outofscopeShown: true,
                                outofscopeOnly: true,
                                outofscopeHidden:specialScoping[serviceName],
                                defaultScope: serviceDefaultScope,
                                pluginName: pluginName,
                                serviceName: serviceName,
                                mapping: pluginDescription.propertiesMapping,
                                frameworkMapping: pluginDescription.fwkPropertiesMapping,
                                hideMissingFrameworkMapping:(serviceName in ['NodeExecutor','FileCopier'])
                                ]}"/>
                          </g:each>
                          <g:unless test="${pluginDescription?.properties?.any{!(it.scope==null||it.scope==PropertyScope.Unspecified? (serviceDefaultScope == PropertyScope.InstanceOnly): (it.scope == PropertyScope.InstanceOnly))}}">
                            %{--no config properties--}%
                            <p class="text-strong">
                              <g:message code="no.configuration" />
                            </p>
                          </g:unless>
                        </div>
                      </div>
                    </div>
                  </g:each>
                </div>
                <g:unless test="${pluginDescList}">
                  <p class="text-strong">
                    <g:message code="none" />
                  </p>
                </g:unless>
              </div>
            </div>
          </div>
          </g:each>
        </div>
      </div>
    </div>
  </div>
</div>
<g:render template="pluginInstallForm" />
</div>
</div>
</body>
</html>
