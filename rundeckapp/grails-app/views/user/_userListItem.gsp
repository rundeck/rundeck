<%@ page import="com.dtolabs.rundeck.server.authorization.AuthConstants" %>
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
  User: greg
  Date: Feb 2, 2010
  Time: 3:08:28 PM
--%>
<tr class="${index!=null && (index%2)==1?'alternateRow':''}">
    <td  style="width:16px">
        <g:expander key="udetail_${user.login}"/>
    </td>
    <td>
        <span class="userlogin" >
            ${user.login}
        </span>
        <span class="username" >
            ${user.firstName} ${user.lastName}
        </span>
        <span class="useremail">
            <g:if test="${user.email}">
                &lt;${user.email}&gt;
            </g:if>
        </span>

        <g:set var="adminauth" value="${auth.resourceAllowedTest(kind:'user',action:[AuthConstants.ACTION_ADMIN],context:'application')}"/>
        <g:if test="${adminauth}">
        <span class="useredit">
            <g:link action="edit" params="[login:user.login]"><g:img file="icon-tiny-edit.png" width="12px" height="12px"/></g:link>
        </span>
        </g:if>
    </td>
</tr>
<tr class="${index!=null && (index%2)==1?'alternateRow':''}" id="udetail_${user.login}" style="display:none">
    <td></td>
    <td >
        <tmpl:user user="${user}" expandAccess="${true}"/>
    </td>
</tr>