%{--
  - Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
<div class="modal" id="${modalid}" tabindex="-1" role="dialog"
     aria-labelledby="${modalid}_title" aria-hidden="true">
    <div class="modal-dialog ${modalsize}">
        <div class="modal-content">
            <div class="modal-header">
                <g:if test="${!noclosebutton}">
                    <button type="button" class="close" data-dismiss="modal"
                            aria-hidden="true">&times;</button>
                </g:if>

                <h4 class="modal-title" id="${modalid}_title">${title}</h4>
            </div>

            <div class="modal-body" id="${modalid}_content">
                ${content}
            </div>

            <div class="modal-footer" id="${modalid}_footer">

                <g:if test="${!nocancel}">
                    <button type="submit" class="btn btn-default" data-dismiss="modal">
                        <g:message code="cancel"/>
                    </button>
                </g:if>
                <span id="${modalid}_buttons">
                    <g:each in="${buttons}" var="button">
                        <button class="btn ${button.css ?: 'btn-default'} " data-bind="${button.bind ?: ''}"
                                onclick="${button.js ?: ''}">
                            ${button.message}
                        </button>
                    </g:each>
                </span>
            </div>
        </div>
    </div>
</div>