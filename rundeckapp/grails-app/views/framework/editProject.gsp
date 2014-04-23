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
    var storageBrowse;
    var storageBrowseTarget;
    function init(){
        configControl=new ResourceModelConfigControl('${prefixKey.encodeAsJavaScript()}');
        configControl.pageInit();
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });

        jQuery('#createform').on('click','.obs-select-storage-path',function(evt) {
            evt.preventDefault();
            var rootPath=jQuery(this).data('storage-root');
            if(!rootPath.startsWith("keys/")){
                rootPath="keys";
            }
            storageBrowseTarget=jQuery(this).data('field');
            console.log("load for "+ rootPath );
            console.log("load target "+storageBrowseTarget);
            if(storageBrowse==null){
                storageBrowse = new StorageBrowser(appLinks.storageKeysApi,rootPath);
                ko.applyBindings(storageBrowse);
                jQuery('#storagebrowse').on('hide.bs.modal',function(){
                    if(storageBrowse && storageBrowse.selectedPath()){
                        jQuery(storageBrowseTarget).val(storageBrowse.selectedPath());
                        storageBrowse.selectedPath(null);
                    }
                });
            }
            storageBrowse.rootPath(rootPath);
            storageBrowse.fileFilter(jQuery(this).data('storage-filter'));
            if(jQuery(storageBrowseTarget).val()){
                storageBrowse.selectedPath(jQuery(storageBrowseTarget).val());
                storageBrowse.path(storageBrowse.parentDirString(jQuery(storageBrowseTarget).val()));
            }else{
                storageBrowse.initialLoad();
            }
        });
    }
    jQuery(init);
    </g:javascript>
</head>

<body>

<g:set var="adminauth"
       value="${auth.resourceAllowedTest(type:'resource',kind:'project',action:['create'],context:'application')}"/>
<g:if test="${adminauth}">

    <div class="row">
    <div class="col-sm-12">
    <div class="alert alert-warning" style="${wdgt.styleVisible(if: (flash.error || request.error || request.errors))}"
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
    </div>
    </div>
    <div class="row">
        <g:form action="saveProject" method="post" onsubmit="return configControl.checkForm();" class="form">
        <div class="col-sm-10 col-sm-offset-1">
            <div class="panel panel-primary"  id="createform">
                <div class="panel-heading">
                        <span class="h3">
                            <g:message code="domain.Project.edit.message"
                                       default="Configure Project"/>: ${(params.project ?: request.project).encodeAsHTML()}
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

    <div class="modal" id="storagebrowse" tabindex="-1" role="dialog" aria-labelledby="storagebrowsetitle"
         aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title" id="storagebrowsetitle">Select a Storage File</h4>
                </div>

                <div class="modal-body" style="max-height: 500px; overflow-y: scroll">
                    <g:render template="storageBrowser"/>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-sm btn-default" data-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-sm btn-success"
                        data-bind="css: selectedPath()?'active':'disabled' "
                        data-dismiss="modal"
                    >
                        Choose Selected File
                    </button>
                </div>
            </div>
        </div>
    </div>

</g:if>
<g:else>
    <div class="row">
    <div class="col-sm-12">
        <div class="alert alert-warning">
            <g:message code="unauthorized.project.create"/>
        </div>
    </div>
    </div>
</g:else>
</body>
</html>
