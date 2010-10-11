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
                First Name:
            </td>
            <td>
                ${user.firstName}
            </td>
        </tr>
        <tr>
            <td>
                Last Name:
            </td>
            <td>
                ${user.lastName}
            </td>
        </tr>
        <tr>
            <td>
                Email:
            </td>
            <td>
                ${user.email}
            </td>
        </tr>
        <tr>
        <td>Username:</td>
        <td>${user.login}</td>
        </tr>
        <g:if test="${user.login == request.remoteUser}">
            <tr>
            <td>Groups:</td>
            <td><%= request.subject.getPrincipals(com.dtolabs.rundeck.core.authentication.Group.class).collect { it.name }.join(", ") %></td>
            </tr>
        </g:if>
    </table>
</div>
