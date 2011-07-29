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

    %{--<script type="text/javascript" src="${resource(dir: 'js', file: 'yellowfade.js')}"></script>--}%
    <g:javascript library="prototype/effects"/>
    <g:javascript>
function _menuDidSelectProject(value){
    //successfully set project value
    if(value){
        document.location="${createLink(action: 'index', controller: 'menu')}";
    }
}
    function addConfigChrome(elem,type,prefix,index,edit){

        var parentNode = $(elem).parentNode;
        var top = new Element("div");
        var wrapper = new Element("div");
        top.appendChild(wrapper);
        $(parentNode).insert(top,{after:elem});
        var content = parentNode.removeChild(elem);

        %{--var hidden = new Element("input");--}%
        %{--hidden.setAttribute("type","hidden");--}%
        %{--hidden.setAttribute("name",prefix+"type");--}%
        %{--hidden.setAttribute("value",type);--}%
        %{--hidden.addClassName("configtype");--}%
        %{--var hidden2 = new Element("input");--}%
        %{--hidden2.setAttribute("type","hidden");--}%
        %{--hidden2.setAttribute("name","prefix");--}%
        %{--hidden2.setAttribute("value",prefix);--}%
        %{--hidden2.addClassName("configprefix");--}%
        var hidden3 = new Element("input");
        hidden3.setAttribute("type","hidden");
        hidden3.setAttribute("name","index");
        hidden3.setAttribute("value",index);
        hidden3.addClassName("configindex");

        var buttons=new Element("div");
        buttons.setStyle({"text-align":"right"});

        if(edit){
            wrapper.addClassName("rounded");

            var button = new Element("button");
            Event.observe(button,'click',function(e){Event.stop(e);editConfig(top,type,prefix,index);});
            button.innerHTML="Edit";

            var cancelbutton = new Element("button");
            Event.observe(cancelbutton,'click',function(e){Event.stop(e);cancelConfig(top);});
            cancelbutton.innerHTML="Delete";

            buttons.appendChild(cancelbutton);
            buttons.appendChild(button);
        }else{
            wrapper.addClassName("popout");
            var button = new Element("button");
            Event.observe(button,'click',function(e){Event.stop(e);checkConfig(top,type,prefix,index);});
            button.innerHTML="Save";
            button.addClassName("needsSave");

            var cancelbutton = new Element("button");
            Event.observe(cancelbutton,'click',function(e){Event.stop(e);cancelConfig(top,type,prefix,index);});
            cancelbutton.innerHTML="Cancel";

            buttons.appendChild(cancelbutton);
            buttons.appendChild(button);
        }

//        content.insert(hidden);
//        content.insert(hidden2);
        content.insert(hidden3);

        wrapper.appendChild(content);

        wrapper.appendChild(buttons);

    }
    function renderConfig(elem,type,prefix,index,revert){
        hidePicker();
        var params=Form.serialize(elem);
        if(revert){
            params+="&revert=true";
        }
        new Ajax.Updater(elem,"${createLink(action: 'viewResourceModelConfig', controller: 'framework')}", {
            parameters:params,
            onComplete:function(ajax){
                if (ajax.request.success()) {
                    addConfigChrome(elem,type,prefix,index,true);
                }
            }
        });
    }
    function checkConfig(elem,type,prefix,index, revert){
        var params=Form.serialize(elem);
        if(revert){
            params+="&revert=true";
        }
        new Ajax.Request("${createLink(action: 'checkResourceModelConfig', controller: 'framework')}", {
            parameters:params,
            evalScripts:true,
            evalJSON:true,
            onSuccess:function(req){
                var data=req.responseJSON;
                if(data.valid){
                    renderConfig(elem,type,prefix,index,revert);
                }else{
                    editConfig(elem,type,prefix,index);
                }
            }
        });
    }
    function editConfig(elem,type,prefix,index){
        var params=Form.serialize(elem);
        new Ajax.Updater(elem,"${createLink(action: 'editResourceModelConfig', controller: 'framework')}", {
            parameters:params,
            onComplete:function(ajax){
                if (ajax.request.success()) {
                    addConfigChrome(elem,type,prefix,index);
                }
            }
        });
    }
    function cancelConfig(elem,type,prefix,index){
        hidePicker();
        var li;
        if(elem.tagName=='li'){
            li=elem;
        }else{
            li=elem.up('li');
        }
        if(li.down('input.isEdit')){
            //discard changes, submit using original values
            checkConfig(elem,type,prefix,index,true);
        }else{
            //cancel new entry
            li.parentNode.removeChild(li);
        }
    }
    var configCount=0;
    function addConfig(type){
        hidePickerAll();
        var num=++configCount;
        var prefix='${prefixKey.encodeAsJavaScript()}.'+num+'.';
        var wrapper = new Element("li");
        var content = new Element("div");
        wrapper.appendChild(content);

        $('configs').appendChild(wrapper);
        new Ajax.Updater(content,"${createLink(action: 'createResourceModelConfig', controller: 'framework')}", {
            parameters:{prefix:prefix,type:type},
            onComplete:function(ajax){
                if (ajax.request.success()) {
                    addConfigChrome(content,type,prefix,num+'');
                }
            }

        }
        );
    }
    function checkForm(){
        if($('configs').down('button.needsSave')){
            $('configs').select('button.needsSave').each(function(e){
                new Effect.Highlight($(e).up('div.popout'));
            });
            return false;
        }
        return true;
    }
    function showPicker(){
        $('sourcebutton').hide();
        $('sourcepicker').show();
    }
    function hidePicker(){
        $('sourcebutton').show();
        $('sourcepicker').hide();
    }
    function hidePickerAll(){
        $$('.sourcechrome').each(Element.hide);
    }
    function init(){
        Event.observe($('sourcebutton'),'click',function(e){Event.stop(e); showPicker();});
        Event.observe($('sourcecancel'),'click',function(e){Event.stop(e); hidePicker();});
        //load widgets for any in-page configs
        var n=1;
        $('configs').select('li div.inpageconfig').each(function(e){
            addConfigChrome(e,null,'${prefixKey}.'+n+'.',n,true);
            n++;
        });
    }
    Event.observe(window, 'load', init);
    </g:javascript>
<style type="text/css">
    #configs li{
        margin-top: 5px;
    }
</style>
</head>

<body>
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
        <g:form action="createProject" method="post" onsubmit="return checkForm();">
            <span class="prompt">
                Create a Project
            </span>

            <table class="simpleForm" cellspacing="0">
                <tr><td><g:message code="domain.Project.field.name" default="Project Name"/>:</td>
                    <td><g:textField name="project" size="50" autofocus="true"/></td></tr>

                <tr>

                    <td>
                        <g:message code="domain.Project.field.resourcesUrl" default="Resources Provider URL"/>:
                    </td>
                    <td>
                        <g:textField name="resourcesUrl" size="50" value="${params.resourcesUrl}"/>
                        <div class="info note">
                            An optional URL to a remote Resource Model Provider.
                        </div>
                    </td>
                </tr>
            </table>
            <g:if test="${resourceModelConfigDescriptions}">

                <span class="prompt">
                    Resource Model Sources:
                </span>

                <div class="presentation">
                    You can add additional custom sources, and their results will be used with the ordering shown.
                    Later sources will override earlier sources.
                </div>

                <ol id="configs">
                    <g:if test="${configs}">
                        <g:each var="config" in="${configs}" status="n">
                            <li>
                                <div class="inpageconfig">
                                    <g:render template="viewResourceModelConfig" model="${[prefix: prefixKey+'.'+(n+1)+'.', values: config.props, includeFormFields: true, description: resourceModelConfigDescriptions.find {it.name==config.type}, saved:true]}"/>
                                    %{--<g:hiddenField name="index" value="${n}"/>--}%
                                </div>
                            </li>
                        </g:each>
                    </g:if>
                </ol>

                <div id="sourcebutton" class="sourcechrome presentation"><button>Add Source</button></div>

                <div id="sourcepicker" class="popout sourcechrome" style="display:none;">
                    <span class="prompt">
                        Choose the type of Source to add:
                    </span>
                    <ul>
                        <g:each in="${resourceModelConfigDescriptions}" var="description">
                            <li>
                                <button onclick="addConfig('${description.name.encodeAsJavaScript()}');
                                return false;">Add</button>
                                <b>${description.title.encodeAsHTML()}</b> - ${description.description.encodeAsHTML()}
                            </li>
                        </g:each>
                    </ul>

                    <div id="sourcecancel" class="sourcechrome presentation"><button>Cancel</button></div>
                </div>
            </g:if>
            <div class="buttons">
                <g:submitButton name="create" value="${g.message(code:'button.action.Create',default:'Create')}" />
            </div>
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