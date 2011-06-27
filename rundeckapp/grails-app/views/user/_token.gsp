%{--
  - Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -        http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%
 <%--
    _token.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 6/20/11 1:48 PM
 --%>


    <td>
        <span class="apitoken">${token.token.encodeAsHTML()}</span>
    </td>
    <td>

        <a style="${wdgt.styleVisible(if: token.token && !(params.showConfirm && params.token==token.token))}"
           class="cleartokenbtn action button"
           href="${createLink(controller: 'user', action: 'clearApiToken', params: [login: user.login,token:token.token])}">
            Remove&hellip;
        </a>

        <div style="${wdgt.styleVisible(if: params.showConfirm && params.token==token.token)}" class="clearconfirm">
            <span
                class="confirmMessage">All clients using this token will lose authentication, are you sure you want to remove this API Token?</span>
            <g:form controller="user" action="clearApiToken">
                <g:hiddenField name="login" value="${user.login}"/>
                <g:hiddenField name="token" value="${token.token}"/>
                <input type="submit" class="no" value="No" name="No"/>
                <input type="submit" class="yes" value="Yes" name="Yes"/>
            </g:form>
        </div>

    </td>
