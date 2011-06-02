%{--
  - Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
    admin.gsp
    
    Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
    Created: 6/1/11 2:22 PM
 --%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <title>Administration</title>
</head>

<body>

<div class="pageTop">
    <div class="floatl">
        <span class="welcomeMessage">Administration</span>
    </div>

    <div class="clear"></div>
</div>

<div class="pageBody">
    <ul>
        <li>
            <g:link controller="menu" action="systemInfo">
                <g:message code="gui.menu.SystemInfo" default="System Information"/>
            </g:link>
        </li>
        <li>
            <g:link controller="user" action="list">
                <g:message code="gui.menu.SystemInfo" default="User Profiles"/>
            </g:link>
        </li>
    </ul>
</div>
</body>
</html>