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
<g:set var="selfprofile" value="${user.login?.equalsIgnoreCase(request.remoteUser)}"/>

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

                        <g:helpTooltip code="security.groups.description" css="text-primary"/>
                    </th>
                </g:if>

            </tr>
            <tr>
                <td>
                    ${user.email}
                    <g:if test="${!user.email}">
                        <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
                    </g:if>
                </td>
                <td>
                    <g:enc>${user.firstName}</g:enc>
                    <g:if test="${!user.firstName}">
                        <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
                    </g:if>
                </td>
                <td>
                    <g:enc>${user.lastName}</g:enc>

                    <g:if test="${!user.lastName}">
                        <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
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


<g:if test="${session.user?.equalsIgnoreCase(user.login) && (tokenAdmin || serviceToken || selfToken)}">
    <div id="gentokensection">
        <div class="row ">
            <div class="col-sm-12">
                <h3>
                    <g:message code="userController.page.profile.heading.userTokens.label"/>
                    <a class="btn btn-xs"
                       data-toggle="modal"
                       href="#gentokenmodal">
                        <g:icon name="plus"/>
                    </a>

                <span class="pull-right">
                    <g:if test="${!tokenAdmin}">

                        <g:form controller="user" action="removeExpiredTokens" useToken="true">
                            <g:hiddenField name="login" value="${user.login}"/>
                            <button type="submit" class="btn btn-sm btn-danger" value="Delete">
                                <g:icon name="remove-circle"/>
                                <g:message code="button.delete.expired.tokens" />
                            </button>
                        </g:form>
                    </g:if>
                    <g:else>

                        <a class="btn btn-sm btn-danger"
                           data-toggle="modal"
                           href="#delexpiredtokenmodal">
                            <i class="fas fa-trash"></i>
                            <g:message code="button.delete.expired.tokens" />
                        </a>
                    </g:else>
                </span>
                </h3>

            </div>
        </div>

        <!-- Delete expired Modal -->
        <div class="modal fade clearconfirm" id="delexpiredtokenmodal" tabindex="-1" role="dialog"
             aria-labelledby="gentokenLabel"
             aria-hidden="true">

            <g:form controller="user" action="removeExpiredTokens" useToken="true">
                <div class="modal-dialog modal-sm">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                            <h4 class="modal-title" id="delexpiredtokenmodalLabel">
                                <g:message code="title.delete.expired.tokens" />
                            </h4>
                        </div>

                        <div class="modal-body">
                            <g:message code="user.profile.modal.delete.expired.tokens.created.by" />
                            <div class="radio">
                            <input type="radio" value="false" name="deleteall" checked id="expired_deleteall_false"/>
                                <label for="expired_deleteall_false">
                                    <g:message code="me" />
                                </label>
                            </div>

                            <div class="radio">
                                <input type="radio" value="true" name="deleteall" id="expired_deleteall"/>
                                <label for="expired_deleteall">
                                    <g:message code="all.users" />
                                </label>
                            </div>
                        </div>

                        <div class="modal-footer">
                            <g:hiddenField name="tokenPagingMax" value="${params.max}"></g:hiddenField>
                            <g:hiddenField name="tokenPagingOffset" value="${params.offset}"></g:hiddenField>
                            <g:hiddenField name="login" value="${user.login}"/>
                            <button type="button" class="btn btn-default" data-dismiss="modal"><g:message
                                    code="button.action.Cancel"/></button>
                            <input type="submit" class="btn btn-danger " value="Delete"
                                   name="${message(code: 'button.action.Delete')}"/>

                        </div>
                    </div><!-- /.modal-content -->
                </div><!-- /.modal-dialog -->
            </g:form>
        </div><!-- /.modal -->

    <!-- Generate Modal -->
        <div class="modal fade clearconfirm" id="gentokenmodal" tabindex="-1" role="dialog"
             aria-labelledby="gentokenLabel"
             aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="gentokenLabel"><g:message
                                code="button.GenerateNewToken.label"/></h4>
                    </div>

                    <div class="modal-body" id="userTokenGenerateForm">
                        <div class="row">
                            <div class="col-sm-12">
                                <div class="form">
                                    <g:if test="${tokenAdmin || serviceToken}">
                                        <div class="form-group">
                                            <label class="control-label" for="tokenuser">
                                                <g:message code="jobquery.title.userFilter"/>
                                            </label>
                                            <input type="text" maxlength="256" name="tokenUser" value="${user.login}"
                                                data-bind="value: tokenUser"
                                                   class="form-control" id="tokenuser"/>

                                            <div class="help-block">
                                                <g:message code="user.profile.generateToken.AuthToken.username.description" />
                                            </div>
                                        </div>
                                    </g:if>
                                    <div class="form-group">
                                        <label class="control-label" for="tokenRoles">
                                            <g:message code="roles"/>
                                        </label>
                                        <g:if test="${tokenAdmin || serviceToken}">
                                            <g:textField type="text" maxlength="256" name="tokenRoles"
                                                data-bind="value: tokenRolesStr"
                                                         class="form-control"
                                                         value="${authRoles?.join(", ")}"/>

                                            <div class="help-block">
                                                <g:message code="roles.token.help"/>
                                            </div>
                                        </g:if>
                                        <g:elseif test="${selfToken}">
                                            <div data-bind="with: roleset">

                                            <span class="textbtn " data-bind="click: checkAll"><g:message code="select.all" /></span>
                                            <span class="textbtn" data-bind="click: uncheckAll"><g:message code="select.none" /></span>
                                            <div class="optionmultiarea" >
                                                <div class="grid " data-bind="foreach: roles">

                                                    <div class="optionvaluemulti ">
                                                        <label class="grid-row optionvaluemulti">
                                                            <span class="grid-cell grid-front">
                                                                <input type="checkbox"
                                                                       name="tokenRoles"
                                                                       checked
                                                                    data-bind="value: name, checked: checked"
                                                                       />
                                                            </span>
                                                            <span class="grid-cell grid-rest" data-bind="text: name">z
                                                            </span>
                                                        </label>
                                                    </div>

                                                </div>
                                            </div>

                                            <div class="help-block">
                                                <g:message code="roles.token.help"/>
                                            </div></div>
                                        </g:elseif>
                                    </label></div>

                                    <div class="form-group">
                                        <label class="control-label">
                                            <g:message code="expiration.in"/>
                                        </label>

                                        <div class="row">
                                            <div class="col-sm-6">

                                                <input type="number"
                                                       name="tokenTime"
                                                       class="form-control"
                                                    data-bind="value: tokenTime"
                                                       placeholder="0"
                                                       min="0"/>

                                                <div class="help-block">
                                                <g:message code="expiration.token.help"
                                                           args="${[tokenMaxExpiration ? g.timeDuration(
                                                                   time: tokenMaxExpiration * 1000L
                                                           ) : 'none']}"/>
                                                </div>
                                            </div>

                                            <div class="col-sm-6">

                                                <select class="form-control col-sm-6" name="tokenTimeUnit" data-bind="value: tokenTimeUnit">
                                                    <option value="m"><g:message
                                                            code="time.unit.minute.plural"/></option>
                                                    <option value="h"><g:message code="time.unit.hour.plural"/></option>
                                                    <option value="d"><g:message code="time.unit.day.plural"/></option>
                                                </select>
                                            </div>
                                        </div>

                                    </div>

                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer">
                        <a class="genusertokenbtn btn btn-success"
                            data-bind="click: actionGenerate"
                           href="${createLink(
                                   controller: 'user',
                                   action: 'generateUserToken',
                                   params: [login: user.login]
                           )}">
                            <g:icon name="plus"/>
                            <g:message code="button.GenerateNewToken.label"/>
                        </a>
                    </div>
                </div><!-- /.modal-content -->
            </div><!-- /.modal-dialog -->
        </div><!-- /.modal -->


        <div class="row userapitoken">
            <div class="col-sm-12">
                <div class="help-block"  data-bind="text: tokenTableSummaryText">

                </div>

                <g:render template="tokenList" model="${[user: user, tokenList: tokens, flashToken: flash.newtoken]}"/>

                <div style="display:none" class="gentokenerror alert alert-danger alert-dismissable">
                    <a class="close" data-dismiss="alert" href="#" aria-hidden="true">&times;</a>
                    <span class="gentokenerror-text"></span>
                </div>

            </div>
        </div>

        <div class="row">
            <div class="col-sm-12">
                <ul class="pagination pagination-sm">
                    <bs:paginate
                            total="${tokenTotal}"
                            controller="user"
                            action="profile"
                            maxsteps="5"
                            prev="&lt;"
                            next="&gt;"/>
                </ul>
            </div>

        </div>

    </div>
</g:if>
