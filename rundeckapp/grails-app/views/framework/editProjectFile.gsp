%{--
  - Copyright 2015 SimplifyOps, Inc. (http://simplifyops.com)
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
   Author: Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <title><g:message code="project.config.editor.title" default="Edit Project File"/></title>

    <g:javascript library="prototype/effects"/>
    <asset:javascript src="leavePageConfirm.js"/>
    <g:jsMessages code="page.unsaved.changes"/>
    <g:javascript>

    function init(){
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
        var confirm = new PageConfirm(message('page.unsaved.changes'));
        jQuery('.apply_ace').each(function () {
            _setupAceTextareaEditor(this,confirm.setNeetsConfirm);
        });
    }
    jQuery(init);
    </g:javascript>
</head>

<body>


    <div class="row">
        <g:form action="saveProjectFile" method="post"
            params="${[project:params.project]}"
                useToken="true"
                onsubmit="" class="form">
        <g:hiddenField name="filename" value="${filename}"/>
        <div class="col-sm-10 col-sm-offset-1">
            <div class="panel panel-primary"  id="createform">
                <div class="panel-heading">
                        <span class="h3">
                            <g:message code="project.file.${filename}.edit.message"
                                       default="Edit {0} for project {1}"
                                       args="${[filename,params.project ?: request.project]}"/>
                    </span>
                </div>
                <div class="panel-body">
                    <div class="help-block">
                        <g:markdown><g:message code="project.file.${filename}.help.markdown"
                                               default="Enter markdown"/></g:markdown>
                    </div>

                    <textarea
                              name="fileText"
                              class="form-control code apply_ace"
                              data-ace-autofocus='true'
                              data-ace-session-mode="markdown"
                              data-ace-height="500px"
                              data-ace-control-soft-wrap="true"
                    >${fileText}</textarea>
                </div>


                <div class="panel-footer">
                    <g:submitButton name="cancel" value="${g.message(code:'button.action.Cancel',default:'Cancel')}" class="btn btn-default reset_page_confirm"/>
                    <g:submitButton name="save" value="${g.message(code:'button.action.Save',default:'Save')}" class="btn btn-primary reset_page_confirm"/>
                </div>
            </div>
        </div>
        </g:form>
    </div>

<!--[if (gt IE 8)|!(IE)]><!--> <g:javascript library="ace/ace"/><!--<![endif]-->
</body>
</html>
