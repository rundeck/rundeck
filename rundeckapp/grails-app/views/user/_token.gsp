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

<g:set var="ukey" value="${g.rkey()}"/>
    <td>
        <span class="apitoken">${token.token.encodeAsHTML()}</span>
    </td>
    <td>

        <a style="${wdgt.styleVisible(if: token.token && !(params.showConfirm && params.token==token.token))}"
           class=" textbtn textbtn-danger"
           data-toggle="modal"
           href="#myModal${ukey}">
            <i class="glyphicon glyphicon-remove-circle"></i>
            Delete&hellip;
        </a>

        <!-- Modal -->
        <div class="modal fade clearconfirm" id="myModal${ukey}" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
             aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title">Delete Token</h4>
                    </div>

                    <div class="modal-body">
                        <p>All clients using this token will lose authentication, are you sure you want to remove this API Token?
                        </p>
                    </div>

                    <div class="modal-footer">
                        <g:form controller="user" action="clearApiToken">
                            <g:hiddenField name="login" value="${user.login}"/>
                            <g:hiddenField name="token" value="${token.token}"/>
                            <button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
                            <input type="submit" class="btn btn-danger yes" value="Delete" name="Delete"/>
                        </g:form>
                    </div>
                </div><!-- /.modal-content -->
            </div><!-- /.modal-dialog -->
        </div><!-- /.modal -->

    </td>
