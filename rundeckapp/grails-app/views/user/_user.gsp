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

<%@ page import="rundeck.AuthToken; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<%--
   _user.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Feb 2, 2010 3:16:55 PM
   $Id$
--%>
<g:set var="selfprofile" value="${user.login == request.remoteUser}"/>

<div class="row">
    <div class="col-sm-12">
        <table class="table table-condensed  table-striped">
            <tr>
                <th class="table-header">
                    <g:message code="domain.User.email.label"/>
                </th>
                <th class="table-header">
                    <g:message code="domain.User.firstName.label"/>
                </th>
                <th class="table-header">
                    <g:message code="domain.User.lastName.label"/>
                </th>
                <g:if test="${selfprofile}">
                    <th class="table-header">
                        <g:message code="security.groups.label"/>

                        <g:helpTooltip code="security.groups.description" css="text-muted"/>
                    </th>
                </g:if>

            </tr>
            <tr>
                <td>
                    ${user.email}
                    <g:if test="${!user.email}">
                        <span class="text-muted small text-uppercase"><g:message code="not.set" /></span>
                    </g:if>
                </td>
                <td>
                    <g:enc>${user.firstName}</g:enc>
                    <g:if test="${!user.firstName}">
                        <span class="text-muted small text-uppercase"><g:message code="not.set" /></span>
                    </g:if>
                </td>
                <td>
                    <g:enc>${user.lastName}</g:enc>

                    <g:if test="${!user.lastName}">
                        <span class="text-muted small text-uppercase"><g:message code="not.set" /></span>
                    </g:if>
                </td>
                <g:if test="${selfprofile}">
                    <td>
                        ${authRoles?.join(", ")}
                    </td>
                </g:if>

            </tr>
        </table>
    </div>

</div>

<g:set var="tokenAdmin" value="${auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.ACTION_ADMIN],
        context: 'application')}"/>
<g:set var="selfToken"
       value="${auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.GENERATE_SELF_TOKEN],
               context: 'application')}"/>
<g:set var="serviceToken"
       value="${auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.GENERATE_SERVICE_TOKEN],
               context: 'application')}"/>

<g:if test="${session.user == user.login && (tokenAdmin || serviceToken)}">
    <g:set var="rkeytok" value="${g.rkey()}"/>
    <div id="${enc(attr: rkeytok)}">
        <div class="row ">
            <div class="col-sm-12">
                <h3>
                    <g:message code="userController.page.profile.heading.userTokens.label"/>
                    <a class="small btn btn-link btn-xs"
                       href="#">
                        <g:icon name="plus"/>
                    </a>
                </h3>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-12">
                <div class="form-inline">

                    <label>
                        <g:message code="jobquery.title.userFilter"/>:
                        <input type="text" maxlength="256" name="tokenUser" value="${user.login}"
                               class="form-control"/>
                    </label>
                    <label>
                        <g:message code="roles"/>:
                        <div class="input-group">
                            <g:textField type="text" maxlength="256" name="tokenRoles"
                                         class="form-control"
                                         value="${authRoles?.join(", ")}"/>
                            <div class="input-group-addon">
                                <g:helpTooltip code="roles.token.help" css="text-muted"/>
                            </div>
                        </div>
                    </label>
                    <label>
                        <g:message code="expiration.in"/>:
                        <div class="input-group">
                            <input type="number" min="0" max="360" name="tokenTime" class="form-control"
                                   placeholder="10"
                                   value="10"/>

                            <div class="input-group-addon">
                                <g:helpTooltip code="expiration.token.help" css="text-muted"/>
                            </div>
                        </div>

                    </label>
                    <select class="form-control" name="tokenTimeUnit">
                        <option value="m"><g:message code="time.unit.minute.plural"/></option>
                        <option value="h"><g:message code="time.unit.hour.plural"/></option>
                        <option value="d"><g:message code="time.unit.day.plural"/></option>
                    </select>
                    <a class="genusertokenbtn small btn btn-link btn-xs"
                       href="${createLink(
                               controller: 'user',
                               action: 'generateUserToken',
                               params: [login: user.login]
                       )}">
                        <g:icon name="plus"/>
                        <g:message code="button.GenerateNewToken.label"/>
                    </a>
                </div>
            </div>
        </div>

        <div class="row userapitoken">
            <div class="col-sm-12">
                <g:set var="tokens" value="${rundeck.AuthToken.findAll()}"/>

                <g:if test="${tokens}">
                    <g:render template="tokenList" model="${[user:user, tokenList:tokens,flashToken:flash.newtoken]}"/>
                </g:if>


                <div style="display:none" class="gentokenerror alert alert-danger alert-dismissable">
                    <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                    <span class="gentokenerror-text"></span>
                </div>

                <g:javascript>
                    fireWhenReady('${enc(js: rkeytok)}',function(){addUserBehavior(true,'${enc(js: rkeytok)}',"${enc(
                        js: user.login
                )}");});
                    fireWhenReady('${enc(js: rkeytok)}',function(){highlightNew('${enc(js: rkeytok)}');});
                    fireWhenReady('${enc(js: rkeytok)}',function(){setLanguage('${enc(js: rkeytok)}');});
                </g:javascript>
            </div>
        </div></div>
</g:if>

<g:if test="${session.user == user.login && (selfToken && !tokenAdmin)}">
    <g:set var="rkeytok" value="${g.rkey()}"/>
    <div id="${enc(attr: rkeytok)}">
        <div class="row ">
            <div class="col-sm-12">
                <h3>
                    <g:message code="userController.page.profile.heading.userTokens.label"/>
                    <a class="small btn btn-link btn-xs"
                       href="#">
                        <g:icon name="plus"/>
                    </a>
                </h3>
            </div>
        </div>
        <div class="row">
            <div class="col-sm-12">
                <table>
                    <tr>
                        <td><g:message code="roles"/>:</td>
                        <td>

                            <select class="form-control tokenRoles" name="tokenRoles" multiple>
                                <g:each var="group" in="${request.subject.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class)}" status="index">
                                    <option value="${group.name}">${group.name}</option>
                                </g:each>
                            </select>
                        </td>
                        <td><g:message code="expiration.in"/>:<g:helpTooltip code="expiration.token.help" css="text-muted"/></td>
                        <td> <input type="number" min="0" max="360" name="tokenTime"/> </td>
                        <td>
                            <select class="form-control" name="tokenTimeUnit">
                                <option value="m"><g:message code="time.unit.minute.plural"/></option>
                                <option value="h"><g:message code="time.unit.hour.plural"/></option>
                                <option value="d"><g:message code="time.unit.day.plural"/></option>
                            </select>
                        </td>
                        <td>
                            <a class="genusertokenbtn small btn btn-link btn-xs"
                               href="${createLink(
                                       controller: 'user',
                                       action: 'generateUserToken',
                                       params: [login: user.login]
                               )}">
                                <g:icon name="plus"/>
                                <g:message code="button.GenerateNewToken.label" />
                            </a>
                        </td>
                    </tr>
                </table>
            </div>
        </div>

        <div class="row userapitoken">
            <div class="col-sm-12">
                <g:set var="tokens" value="${rundeck.AuthToken.findAllByUser(user)}"/>


                <g:if test="${tokens}">
                    <g:render template="tokenList" model="${[user:user, tokenList:tokens,flashToken:flash.newtoken]}"/>
                </g:if>


                <div style="display:none" class="gentokenerror alert alert-danger alert-dismissable">
                    <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                    <span class="gentokenerror-text"></span>
                </div>

                <g:javascript>
                    fireWhenReady('${enc(js: rkeytok)}',function(){addUserBehavior(false,'${enc(js: rkeytok)}',"${enc(
                        js: user.login
                )}");});
                    fireWhenReady('${enc(js: rkeytok)}',function(){highlightNew('${enc(js: rkeytok)}');});
                    fireWhenReady('${enc(js: rkeytok)}',function(){setLanguage('${enc(js: rkeytok)}');});
                </g:javascript>
            </div>
        </div></div>
</g:if>