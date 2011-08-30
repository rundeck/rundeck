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
    <title><g:message code="domain.Project.choose.title" default="Create a Project"/></title>

    <g:javascript library="prototype/effects"/>
    <g:javascript library="resourceModelConfig"/>
    <g:javascript>
    function _menuDidSelectProject(value){
        //successfully set project value
        if(value){
            document.location="${createLink(action: 'index', controller: 'menu')}";
        }
    }
    var configControl;
    function init(){
        configControl=new ResourceModelConfigControl('${prefixKey.encodeAsJavaScript()}');
        configControl.pageInit();
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
    }
    Event.observe(window, 'load', init);
    </g:javascript>
<style type="text/css">
    #configs li{
        margin-top: 5px;
    }
    div.buttons{
        text-align:right;
    }
</style>
</head>

<body>
<div class="pageTop">
    <span class="welcomeMessage floatl">
        <g:message code="domain.Project.create.message" default="Create a new Project"/>
    </span>
</div>
<g:set var="adminauth"
       value="${auth.resourceAllowedTest(type:'resource',kind:'project',action:['create'],context:'application')}"/>
<g:if test="${adminauth}">
    <div class="pageBody form note error" style="${wdgt.styleVisible(if: (flash.error || request.error || request.errors))}"
         id="editerror">
        ${flash.error?.encodeAsHTML()}${request.error?.encodeAsHTML()}
        <g:if test="${request.errors}">
            <ul>
                <g:each in="${request.errors}" var="err">
                    <g:if test="${err}">
                        <li>${err.encodeAsHTML()}</li>
                    </g:if>
                </g:each>
            </ul>
        </g:if>
    </div>
    <div class="pageBody form" style="width:500px;" id="createform">

        <g:form action="createProject" method="post" onsubmit="return configControl.checkForm();">
            <tmpl:editProjectForm />
            <div class="buttons">
                <g:submitButton name="create" value="${g.message(code:'button.action.Create',default:'Create')}" />
            </div>
        </g:form>
    </div>
</g:if>
<g:else>
    <div class="pageBody">
        <div class="error note"><g:message code="unauthorized.project.create"/></div>
    </div>
</g:else>
</body>
</html>