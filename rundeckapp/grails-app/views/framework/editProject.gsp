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
    <meta name="tabpage" content="configure"/>
    <title><g:message code="domain.Project.choose.title" default="Edit Project"/></title>

    <g:javascript library="prototype/effects"/>
    <g:javascript library="resourceModelConfig"/>
    <asset:javascript src="storageBrowseKO.js"/>
    <g:javascript>

    var configControl;
    function init(){
        configControl=new ResourceModelConfigControl('${enc(js:prefixKey)}');
        configControl.pageInit();
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
        jQuery('#storagebrowse').find('.obs-storagebrowse-select').on('click',function(evt){
            if(jQuery(evt.delegateTarget).hasClass('active')){
                var storageBrowse = jQuery('#storagebrowse').data('storageBrowser');
                var storageBrowseTarget = storageBrowse.fieldTarget();
                if(storageBrowse && storageBrowse.selectedPath()){
                    jQuery(storageBrowseTarget).val(storageBrowse.selectedPath());
                    storageBrowse.selectedPath(null);
                }
            }
        });
    }
    jQuery(init);
    </g:javascript>
</head>

<body>


    <div class="row">
        <g:form action="saveProject" method="post"
                useToken="true"
                onsubmit="return configControl.checkForm();" class="form">
        <div class="col-sm-10 col-sm-offset-1">
            <div class="panel panel-primary"  id="createform">
                <div class="panel-heading">
                        <span class="h3">
                            <g:message code="domain.Project.edit.message"
                                       default="Configure Project"/>: <g:enc>${params.project ?: request.project}</g:enc>
                    </span>
                </div>
                <g:render template="editProjectForm" model="${[editOnly:true,project: params.project ?: request.project]}"/>
                <div class="panel-footer">
                    <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default"/>
                    <g:submitButton name="save" value="${g.message(code:'button.action.Save',default:'Save')}" class="btn btn-primary"/>
                </div>
            </div>
        </div>
        </g:form>
    </div>

    <g:render template="storageBrowseModalKO"/>
</body>
</html>
