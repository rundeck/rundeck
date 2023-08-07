<g:if test="${flash.error || flash.errorCode}">
    <div class="alert alert-danger alert-dismissable">
        <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
        ${flash.error?.toString()}
        <g:if test="${flash.errorCode ?: request.errorCode}">
            <g:message code="${flash.errorCode ?: request.errorCode}"
                       args="${flash.errorArgs ?: request.errorArgs}"/>
        </g:if>
        <g:if test="${flash.invalidToken}">
            <g:message code="request.error.invalidtoken.message"/>
        </g:if>
    </div>
</g:if>
<div class="container-fluid">
    <div class="row">
        <div class="col-xs-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">
                        <g:message code="rdpro.license.page.index.h1.title"/>
                        <g:if test="${license.active}">
                            <span class="text-success">
                                <i class="text-success glyphicon glyphicon-ok-circle"></i>
                                <g:message code="rdpro.license.state.title.${license.state}"/>
                            </span>
                        </g:if>
                        <g:else>
                            <span class="text-warning">
                                <g:message code="rdpro.license.state.title.${license.state}"/>
                            </span>
                        </g:else>
                    </h3>
                </div>

                <div class="card-content">

                    <div class="row">
                        <div class="col-xs-12">

                            <g:if test="${!license.active}">

                                <g:if test="${license.invalidCode}">
                                    <div class="h3 text-danger">
                                        <i class="text-danger glyphicon glyphicon-warning-sign"></i>
                                        <g:message
                                                code="rdpro.license.state.invalid.${license.invalidCode}.description"/>
                                    </div>
                                </g:if>
                                <g:else>
                                    <div class="h3 text-danger">
                                        <i class="text-danger glyphicon glyphicon-warning-sign"></i>
                                        <g:message code="rdpro.license.state.description.${license.state}"/>
                                    </div>
                                </g:else>
                            </g:if>
                            <g:if test="${license.shouldWarn}">
                                <div class="text-warning">
                                    <g:message code="rdpro.license.expiration.${license.state}.warning"
                                               args="${[license.remaining.toString()]}"/>
                                </div>
                            </g:if>
                            <g:if test="${needLicense}">
                                <div class="well text-info">
                                    <g:markdown><g:message code="rdpro.license.howto_activate.uuid.md"
                                                           args="${[serverUUID ?: '', edition]}"/></g:markdown>
                                </div>
                            </g:if>

                            <g:if test="${hasLicense && authorized}">
                                <table class="table table-bordered">
                                    <tr>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.state.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.company.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.contactEmail.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.type.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.editions.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.applicationVersion.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.issueDate.title"/></th>
                                        <th class="text-muted text-center text-header"><g:message
                                                code="rdpro.license.license.validUntil.title"/></th>
                                    </tr>
                                    <tr>
                                        <td class="${license.active ? 'text-success' : 'text-warning'}">
                                            <g:if test="${license.state}">
                                                <g:message code="rdpro.license.state.title.${license.state}"/>
                                            </g:if>
                                        </td>
                                        <td>${license.company}</td>
                                        <td>${license.contactEmail}</td>
                                        <td>${license.type}</td>
                                        <td>
                                            <g:if test="${license.editions?.size() > 1}">
                                                <ul>
                                                    <g:each in="${license.editions}" var="edition">
                                                        <li>${edition}</li>
                                                    </g:each>
                                                </ul>
                                            </g:if>
                                            <g:else>
                                                ${license.editions ? license.editions[0] : '-'}
                                            </g:else>
                                        </td>
                                        <td>
                                            <g:if test="${license.applicationVersion?.size()>1}">
                                                <ul>
                                                    <g:each in="${license.applicationVersion}" var="applicationVersion">
                                                        <li>${applicationVersion}</li>
                                                    </g:each>
                                                </ul>
                                            </g:if>
                                            <g:else>
                                                ${license.applicationVersion ? license.applicationVersion[0] : '-'}
                                            </g:else>
                                        </td>
                                        <td>${license.issueDate}</td>
                                        <td class="${license.state == 'GRACE' ? 'text-warning' :
                                                license.state == 'EXPIRED' ? 'text-danger' : 'text-success'}">
                                            <g:if test="${license.perpetual}">
                                                &infin;
                                            </g:if>
                                            <g:else>
                                                ${license.validUntil}
                                            </g:else>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-muted" colspan="8">
                                            <g:message code="rdpro.license.license.serverUUIDs.title"/>:
                                            <ul>
                                                <g:each in="${license.serverUUIDs}" var="uuid">
                                                    <li class="${uuid == serverUUID ? 'text-success' : ''}">
                                                        ${uuid}
                                                        <g:if test="${uuid == serverUUID}">
                                                            <span class="badge badge-success"><g:message
                                                                    code="rdpro.license.badge.this.server.text"/></span>
                                                        </g:if>
                                                    </li>
                                                </g:each>
                                            </ul>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="text-muted" colspan="8">
                                            <g:message
                                                    code="rdpro.license.license.licenseId.title"/>: ${license.licenseId}
                                        </td>
                                    </tr>
                                    <g:if test="${license.entitlements}">
                                        <g:each in="${license.entitlements}" var="entitlement">
                                            <tr>
                                                <td colspan="2">
                                                    ${entitlement.description?.title?:entitlement.name}
                                                    <div class="help-block">
                                                        <g:render template="/scheduledExecution/description"
                                                                  model="[
                                                                          description:
                                                                                  entitlement.description?.description,
                                                                          mode       : 'collapsed',
                                                                  ]"/>
                                                    </div>
                                                </td>
                                                <td class="text-muted" colspan="6">
                                                    <g:if test="${entitlement.description?.properties}">
                                                        <g:each in="${entitlement.description?.properties}" var="prop">
                                                            <g:render template="/framework/pluginConfigPropertySummaryValue"
                                                                      model="${[
                                                                              prop  : prop,
                                                                              prefix: '',
                                                                              values: entitlement.value,
                                                                      ]}"/>
                                                        </g:each>
                                                    </g:if>
                                                </td>
                                            </tr>
                                        </g:each>
                                    </g:if>
                                </table>
                            </g:if>
                            <g:elseif test="${authorized}">
                                <div class="well text-warning">
                                    <g:message code="rdpro.license.page.index.license.not_found.warning"/>
                                </div>
                            </g:elseif>
                            <g:elseif test="${!hasLicense && !authorized}">
                                <div class="well text-warning">
                                    <g:message code="rdpro.license.page.index.license.not_found_no_auth.warning"/>
                                </div>
                            </g:elseif>

                        </div>
                    </div>
                </div>

                <div class="panel-footer">
                    <div class="row">
                        <div class="col-xs-6">
                            <g:if test="${authorized}">
                                <button class="btn ${highlightButton ? 'btn-primary' : 'btn-link'}" data-toggle="modal"
                                        data-target="#upload"><g:message
                                        code="rdpro.license.page.index.form.upload.button.title"/>
                                    <i class="glyphicon glyphicon-upload"></i>
                                </button>
                            </g:if>
                        </div>

                        <div class="col-xs-6 text-right">
                            <g:markdown><g:message code="rdpro.license.footer.info.md"/></g:markdown>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    </div>
</div>
<g:if test="${authorized}">

    <div class="modal fade" id="upload" tabindex="-1" role="dialog" aria-labelledby="uploadtitle"
         aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <g:uploadForm action="addLicense" method="POST" class="form" role="form" useToken="true">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                aria-hidden="true">&times;</span></button>
                        <h4 class="modal-title" id="uploadtitle">
                            <g:message code="rdpro.license.page.index.form.licenseFile.label"/>
                        </h4>
                    </div>

                    <div class="modal-body">
                        <div class="form-group">
                            <label for="licenseFile">
                                <g:message code="rdpro.license.page.index.form.licenseFile.help"/>
                            </label>
                            <input type="file" name="licenseFile" id="licenseFile" class="form-control"/>

                        </div>

                    </div>

                    <div class="modal-footer">
                        <button type="button" class="btn btn-default" data-dismiss="modal"><g:message
                                code="cancel"/></button>
                        <button type="submit" class="btn btn-cta"><g:message
                                code="rdpro.license.page.index.form.upload.button.title"/></button>
                    </div>
                </g:uploadForm>
            </div>
        </div>
    </div>

</g:if>