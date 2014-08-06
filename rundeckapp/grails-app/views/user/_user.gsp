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
<div class="presentation">
    <table class="simpleForm">
        <tr>
            <td>
                Email:
            </td>
            <td>
                ${user.email}
                <g:if test="${edit}">

                    <small>
                        <g:link action="edit" params="[login: user.login]"
                                class=" textbtn textbtn-default btn-sm textbtn-on-hover">
                            <i class="glyphicon-edit glyphicon"></i>
                            Edit
                        </g:link>
                    </small>
                </g:if>
            </td>
        </tr>
        <g:if test="${user.login == request.remoteUser}">
            <tr>
                <td>Groups:</td>
                <td><%=request.subject.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class).collect { it.name }.join(", ")%></td>
            </tr>
        </g:if>
        <g:if test="${session.user==user.login && (auth.resourceAllowedTest(kind:'user',action:[AuthConstants.ACTION_ADMIN],context:'application'))}">
            <g:set var="rkeytok" value="${g.rkey()}"/>
            <tr id="${rkeytok}" class="userapitoken">
                <td>API Tokens:</td>
                <td>
                    <g:set var="tokens" value="${rundeck.AuthToken.findAllByUser(user)}"/>


                    <table class="apitokentable">
                        <tbody >
                            <g:if test="${tokens}">
                            <g:each var="tokenobj" in="${tokens}">
                                <tr class="apitokenform ${tokenobj.token == flash.newtoken?'newtoken':''}" style="${tokenobj.token== flash.newtoken?'opacity:0;':''}">
                                <g:render template="token" model="${[user:user,token:tokenobj]}"/>
                                </tr>
                            </g:each>
                            </g:if>
                        </tbody>
                    </table>
                    <div style="margin-top:10px;" >
                        <a class="gentokenbtn textbtn textbtn-default btn-xs"
                           href="${createLink(controller: 'user', action: 'generateApiToken', params: [login: user.login])}">
                            <i class="glyphicon glyphicon-plus"></i>
                            Generate New Token
                        </a>
                    </div>

                    <div style="display:none" class="gentokenerror error note">
                    </div>

                    <g:javascript>
                    fireWhenReady('${rkeytok}',function(){addBehavior('${rkeytok}',"${enc(js:user.login)}");});
                    fireWhenReady('${rkeytok}',function(){highlightNew('${rkeytok}');});
                    </g:javascript>
                </td>
            </tr>
        </g:if>
    </table>
</div>
