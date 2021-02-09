%{--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

<ul class="dropdown-menu dropdown-menu-right">
  <li>
    <div style="padding: 10px 15px;">
      Hi ${session.user}!
    </div>
  </li>

    <g:ifMenuItems type="USER_MENU">
        <li role="separator" class="divider"></li>
    </g:ifMenuItems>
    <g:forMenuItems type="USER_MENU" var="item">
        <li>
            <a href="${enc(attr:item.href)}"
                 title="${enc(attr:g.message(code:item.titleCode,default:item.title))}">
                <g:message code="${item.titleCode}" default="${item.title}"/>
            </a>
        </li>
    </g:forMenuItems>

  <li role="separator" class="divider"></li>
  <li>
    <g:link controller="user" action="profile">
      <g:message code="profile"/>
    </g:link>
  </li>
  <li>
    <g:link controller="user" action="logout">
      <g:message code="logout"/>
    </g:link>
  </li>
</ul>
