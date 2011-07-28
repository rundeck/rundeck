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
   chooseProject.gsp

   Author: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
   Created: Dec 29, 2010 6:28:51 PM
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="domain.Project.choose.title" default="Choose a Project"/></title>
</head>
<body>
<g:javascript>
function _menuDidSelectProject(value){
    //successfully set project value
    if(value){
        document.location="${createLink(action: 'index', controller: 'menu')}";
    }
}
</g:javascript>
<div class="pageTop">
    <span class="welcomeMessage floatl">
        %{--<g:message code="domain.Project.welcome.message" default="Welcome to RunDeck"/>--}%
        <g:message code="domain.Project.create.message" default="Please create a new Project"/>
    </span>
</div>

%{--<div class="pageBody">--}%
%{--<table class="projectselect">--}%
%{--<g:each var="project" in="${projects}">--}%
%{--<tr>--}%
%{--<td>--}%
%{--&bull; <span class="action textbtn" onclick="selectProject('${project.name.encodeAsJavaScript()}');">${project.name}</span>--}%
%{--</td>--}%
%{--<td>${project.hasProperty('project.description') ? project.getProperty('project.description') : ''}</td>--}%
%{--</tr>--}%
%{--</g:each>--}%
%{--</table>--}%

%{--<g:if test="${projects}">--}%
%{--<g:ifUserInAnyRoles roles="admin">--}%
%{--<span class="textbtn button action" onclick="Element.show('createform');Element.hide(this);"><g:message code="domain.Project.create.button" default="Create a Project"/></span>--}%
%{--</g:ifUserInAnyRoles>--}%
%{--</g:if>--}%
%{--<g:else>--}%
%{--<g:ifUserInAnyRoles roles="admin" member="false">--}%
%{--<div class="error note">You are not authorized to create a project. Ask your RunDeck admin to create one.</div>--}%
%{--</g:ifUserInAnyRoles>--}%
%{--</g:else>--}%
%{--</div>--}%
<g:ifUserInAnyRoles roles="admin">
    <div class="pageBody form" style="width:500px;" id="createform">
        <div class="note error" style="${wdgt.styleVisible(if: (flash.error || request.error))}" id="editerror">
            ${flash.error}${request.error}
        </div>
        <g:form action="createProject" method="post">
            <table class="simpleForm" cellspacing="0">
                <tr><td><g:message code="domain.Project.field.name" default="Project Name"/>:</td>
                    <td><g:textField name="project" size="50" autofocus="true"/></td></tr>
                %{--<tr><td>Description:</td> <td><g:textField name="description" size="50"/></td></tr>--}%
                <tr>
                <td colspan="2" style="text-align: left;">
                    <input type="checkbox" name="customResources" onchange="$('resourcesProviders').show();" id="custom"/>
                    <label for="custom">
                        <g:message code="domain.Project.field.resourcesUrlPrompt" default="Include custom Resources Providers?"/>
                    </label>
                </td>
                </tr>
                <tbody id="resourcesProviders" style="display:none">
                <tr>
                <td><g:message code="domain.Project.field.resourcesUrl" default="Resources Provider URL"/>:</td>
                <td><g:textField name="resourcesUrl" size="50"/></td>
                </tr>
                </tbody>
            </table>
            <div class="buttons"><g:submitButton name="create" value="${g.message(code:'button.action.Create',default:'Create')}"/></div>
        </g:form>
    </div>
</g:ifUserInAnyRoles>
<g:ifUserInAnyRoles roles="admin" member="false">
    <div class="pageBody">
        <div class="error note"><g:message code="unauthorized.project.create"/></div>
    </div>
</g:ifUserInAnyRoles>
</body>
</html>