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
    _token.gsp

    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 6/20/11 1:48 PM
 --%>

<g:set var="ukey" value="${g.rkey()}"/>
<tr class="apitokenform ${token.token == flashToken ? 'newtoken' : ''}"
    id="token-${token.uuid}"
    style="${token.token == flashToken ? 'opacity:0;' : ''}">
    <td width="20%" class="token-data-holder">
        <g:if test="${token.uuid}">
            <span class="btn btn-xs collapse in obs_reveal_token" data-token-id="${token.uuid}">
                <g:icon name="eye-open"/>
                <g:message code="show.token"/>
            </span>
            <code class="token-data-holder obs_hide_token collapse" data-token-id="${token.uuid}"></code>
        </g:if>
        <g:else>
            <span class="btn btn-simple btn-xs collapse in"
                  id="${ukey}_rvbtn"
                  data-toggle="collapse"
                  data-target="#${ukey}_reveal,#${ukey}_rvbtn">
                <g:icon name="eye-open"/>
                <g:message code="show.token"/>
            </span>
            <code class="token-data-holder collapse" id="${ukey}_reveal">${token.token}</code>
        </g:else>
    </td>
        <td width="8%">
            <g:if test="${token.expiration}">
                <g:relativeDate elapsed="${token.expiration}" untilClass="timeuntil" agoClass="text-warning"/>
            </g:if>
        </td>
        <td width="22%">
            <g:if test="${token.expiration}">
                <span class="text-primary"><g:formatDate date="${token.expiration}"/></span>
            </g:if>
        </td>
        <td width="10%" title="Creator: ${token.creator}">
            ${token.user.login}
            <g:if test="${!token?.user?.login?.equalsIgnoreCase(token.creator)}">
                (${token.creator})
            </g:if>
        </td>
        <td width="30%" class="ellipsis" title="${token.authRoles}">
            <span>${token.authRoles}</span>
        </td>
        <td width="10%">
          <a style="padding-left:14px; padding-right: 14px; ${wdgt.styleVisible(if: token.token && !(params.showConfirm && params.token==token.token))}"
             class="btn btn-sm btn-danger"
             data-toggle="modal"
             href="#myModal${enc(attr:ukey)}">
             <i class="fas fa-trash"></i>
              <g:message code="button.action.Delete" />
          </a>

        <!-- Modal -->

        <g:form controller="user" action="clearApiToken" useToken="true">
            <div class="modal fade clearconfirm" id="myModal${enc(attr: ukey)}" tabindex="-1" role="dialog"
                 aria-labelledby="myModalLabel"
                 aria-hidden="true">
                <div class="modal-dialog modal-sm">
                    <div class="modal-content">
                        <div class="modal-header">
                            <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                            <h4 class="modal-title"><g:message
                                    code="userController.page.profile.heading.delete.token.title"/></h4>
                        </div>

                        <div class="modal-body">
                            <p><g:message code="userController.page.profile.delete.token.description"/></p>
                        </div>

                        <div class="modal-footer">
                            <g:hiddenField name="tokenPagingMax" value="${params.max}"></g:hiddenField>
                            <g:hiddenField name="tokenPagingOffset" value="${params.offset}"></g:hiddenField>
                            <g:hiddenField name="login" value="${user.login}"></g:hiddenField>
                            <g:if test="${token.uuid}">
                                <g:hiddenField name="tokenid" value="${token.uuid}"></g:hiddenField>
                            </g:if>
                            <g:else>
                                <g:hiddenField name="token" value="${token.token}"></g:hiddenField>
                            </g:else>
                            <button type="button" class="btn btn-default" data-dismiss="modal"><g:message
                                    code="button.action.Cancel"/></button>
                            <input type="submit" class="btn btn-danger yes" value="Delete"
                                   name="${message(code: 'button.action.Delete')}">

                        </div>
                    </div><!-- /.modal-content -->
                </div><!-- /.modal-dialog -->
            </div><!-- /.modal -->
        </g:form>
    </td>
</tr>
