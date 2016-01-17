<%@ page import="rundeck.AuthToken; com.dtolabs.rundeck.server.authorization.AuthConstants" %>
<%--
 Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 --%>
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
                <th>
                    <g:message code="domain.User.email.label"/>
                </th>
                <th>
                    <g:message code="domain.User.firstName.label"/>
                </th>
                <th>
                    <g:message code="domain.User.lastName.label"/>
                </th>
                <g:if test="${selfprofile}">
                    <th>
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
                        ${request.subject.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class).
                                collect { it.name }.
                                join(", ")}
                    </td>
                </g:if>

            </tr>
        </table>
    </div>

</div>

<g:if test="${session.user == user.login &&
        (auth.resourceAllowedTest(kind: 'user', action: [AuthConstants.ACTION_ADMIN], context: 'application'))}">
    <g:set var="rkeytok" value="${g.rkey()}"/>
    <div id="${enc(attr: rkeytok)}">
        <div class="row ">
            <div class="col-sm-12">
                <h3>
                    <g:message code="userController.page.profile.heading.apiTokens.label"/>
                    <a class="gentokenbtn small btn btn-link btn-xs"
                       href="${createLink(
                               controller: 'user',
                               action: 'generateApiToken',
                               params: [login: user.login]
                       )}">
                        <g:icon name="plus"/>
                        <g:message code="button.GenerateNewToken.label" />
                    </a>
                </h3>
            </div>
        </div>

        <div class="row userapitoken">
            <div class="col-sm-12">
                <g:set var="tokens" value="${rundeck.AuthToken.findAllByUser(user)}"/>


                <ul class="apitokentable list-unstyled">
                    <g:if test="${tokens}">
                        <g:each var="tokenobj" in="${tokens}">
                            <li class="apitokenform ${tokenobj.token == flash.newtoken ? 'newtoken' : ''}"
                                style="${tokenobj.token == flash.newtoken ? 'opacity:0;' : ''}">
                                <g:render template="token" model="${[user: user, token: tokenobj]}"/>
                            </li>
                        </g:each>
                    </g:if>
                </ul>


                <div style="display:none" class="gentokenerror alert alert-danger alert-dismissable">
                    <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                    <span class="gentokenerror-text"></span>
                </div>

                <g:javascript>
                    fireWhenReady('${enc(js: rkeytok)}',function(){addBehavior('${enc(js: rkeytok)}',"${enc(
                        js: user.login
                )}");});
                    fireWhenReady('${enc(js: rkeytok)}',function(){highlightNew('${enc(js: rkeytok)}');});
                </g:javascript>
            </div>
        </div></div>
</g:if>
