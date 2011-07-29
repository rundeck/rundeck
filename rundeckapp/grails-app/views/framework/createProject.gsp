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
    function addConfigChrome(elem,type,prefix){

        var parentNode = elem.parentNode;
        var top = new Element("div");
        var wrapper = new Element("div");
        top.appendChild(wrapper);
        $(parentNode).insert(top,{after:elem});
        var content = parentNode.removeChild(elem);

        wrapper.addClassName("popout");

        var hidden = new Element("input");
        hidden.setAttribute("type","hidden");
        hidden.setAttribute("name",prefix+"type");
        hidden.setAttribute("value",type);
        hidden.addClassName("configtype");
        var hidden2 = new Element("input");
        hidden2.setAttribute("type","hidden");
        hidden2.setAttribute("name","prefix");
        hidden2.setAttribute("value",prefix);
        hidden2.addClassName("configprefix");

        var buttons=new Element("div");
        buttons.setStyle({"text-align":"right"});

        var button = new Element("button");
        Event.observe(button,'click',function(e){Event.stop(e);saveConfig(top,type,prefix);});
        button.innerHTML="Save";

        var cancelbutton = new Element("button");
        Event.observe(cancelbutton,'click',function(e){Event.stop(e);cancelConfig(top);});
        cancelbutton.innerHTML="Cancel";

        buttons.appendChild(cancelbutton);
        buttons.appendChild(button);

        content.insert(hidden);
        content.insert(hidden2);

        wrapper.appendChild(content);

        wrapper.appendChild(buttons);

    }
    function saveConfig(elem,type,prefix){
        var params=Form.serialize(elem);
        new Ajax.Updater(elem,"${createLink(action: 'saveResourceModelConfig', controller: 'framework')}", {
            parameters:params,
            onComplete:function(ajax){
                if (ajax.request.success()) {
                    addConfigChrome(elem,type,prefix);
                }
            }
        });
    }
    function cancelConfig(elem){
        elem.parentNode.removeChild(elem);
    }
    var configCount=0;
    function addConfig(type){
        var num=++configCount;
        var prefix='${prefixKey.encodeAsJavaScript()}.'+num+'.';
        var wrapper = new Element("div");
        wrapper.addClassName("configForm");

        $('configs').appendChild(wrapper);
        new Ajax.Updater(wrapper,"${createLink(action:'createResourceModelConfig',controller:'framework')}", {
            parameters:{prefix:prefix,type:type},
            onComplete:function(ajax){
                if (ajax.request.success()) {
                    addConfigChrome(wrapper,type,prefix);
                }
            }
            
        }
        );
    }
</g:javascript>
<div class="pageTop">
    <span class="welcomeMessage floatl">
        %{--<g:message code="domain.Project.welcome.message" default="Welcome to RunDeck"/>--}%
        <g:message code="domain.Project.create.message" default="Please create a new Project"/>
    </span>
</div>

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
            <g:if test="${resourceModelConfigDescriptions}">
                <span class="prompt">
                    Add additional Resource Model sources:
                </span>
                <g:each in="${resourceModelConfigDescriptions}" var="description">
                    <li>
                        <button onclick="addConfig('${description.name.encodeAsJavaScript()}');
                        return false;">Add</button>
                        <b>${description.title.encodeAsHTML()}</b> - ${description.description.encodeAsHTML()}
                    </li>
                </g:each>
                <div id="configs">

                </div>
            </g:if>
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