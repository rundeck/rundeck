%{-- - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com) - - Licensed under the Apache License, Version 2.0 (the "License"); - you may not use this file except in compliance with the License. - You may obtain a copy of the License at - -
http://www.apache.org/licenses/LICENSE-2.0 - - Unless required by applicable law or agreed to in writing, software - distributed under the License is distributed on an "AS IS" BASIS, - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. - See the License for the specific language governing permissions and - limitations under the License. --}%

<%--
  Created by IntelliJ IDEA.
  User: greg
  Date: 9/29/14
  Time: 3:04 PM
  To change this template use File | Settings | File Templates.
--%>

  <%@ page contentType="text/html;charset=UTF-8" %>
    <html xmlns="http://www.w3.org/1999/html">
      <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="base"/>
        <meta name="tabpage" content="configure"/>
        <meta name="tabtitle" content="${g.message(code: 'gui.menu.KeyStorage')}"/>
        <title><g:message code="gui.menu.KeyStorage"/></title>
        <g:set var="downloadenabled" value="${cfg.getBoolean(config: "gui.keystorage.downloadenabled", default: true)}"/>

        <g:embedJSON id="storageData" data="[
                resourcePath:params.resourcePath,
                project:params.project,
                downloadenabled: downloadenabled

        ]"/>
        <feature:disabled name="vueKeyStorage">
            <asset:javascript src="menu/storage.js"/>
        </feature:disabled>
        <feature:enabled name="vueKeyStorage">
            <asset:javascript src="static/pages/storage.js" defer="defer"/>
            <g:javascript>
                window._rundeck = Object.assign(window._rundeck || {}, {
                    data: { }
                });
            </g:javascript>
        </feature:enabled>
      </head>

    <feature:disabled name="vueKeyStorage">
        <body>
        <div class="content">
            <div id="layoutBody">
                <div class="title">
                    <span class="text-h3"><i class="fas fa-key"></i> ${g.message(code:"gui.menu.KeyStorage")}</span>
                </div>
                <div id="page_storage" class="container-fluid">
                    <div class="row">
                        <div class="col-sm-12">
                            <g:render template="/common/messages"/>
                        </div>
                    </div>
                    <div class="row">

                        <div class="col-xs-12">
                            <div class="card">
                                <div class="card-content">
                                    <g:render template="/framework/storageBrowser"/>
                                </div>
                                <div class="card-footer">
                                    <hr>
                                    <span class=" text-info">
                                        <g:message code="page.keyStorage.description"/>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                %{--modal file delete confirmation--}%
                <div class="modal" id="storageconfirmdelete" tabindex="-1" role="dialog" aria-labelledby="storageconfirmdeletetitle" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                                <h4 class="modal-title" id="storageconfirmdeletetitle"><g:message code="storage.delete.selected.key"/></h4>
                            </div>

                            <div class="modal-body">
                                <p><g:message code="storage.delete.selected.key.confirm"/></p>

                                <p>
                                    <strong data-bind="text: selectedPath()" class="text-info"></strong>
                                </p>
                            </div>

                            <div class="modal-footer">
                                <button type="button" class="btn btn-sm btn-default" data-dismiss="modal"><g:message code="cancel"/></button>

                                <button data-bind=" click: $root.delete" data-dismiss="modal" class="btn btn-sm btn-danger obs-storagedelete-select"><g:message code="delete"/></button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal" id="storageuploadkey" tabindex="-1" role="dialog" aria-labelledby="storageuploadtitle2" aria-hidden="true">
                    <div class="modal-dialog">
                        %{--modal storage key upload/input form--}%
                        <div class="modal-content">
                            <g:uploadForm controller="storage" action="keyStorageUpload" id="uploadKeyForm" useToken="true" class="form-horizontal" role="form"
                                          params="[project:params.project]">
                                <div class="modal-header">
                                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                                    <h4 class="modal-title" id="storageuploadtitle2" data-bind="if: !upload.modifyMode()">
                                        <g:message code="storage.add.or.upload.a.key"/>
                                    </h4>
                                    <h4 class="modal-title" id="storageuploadtitle3" data-bind="if: upload.modifyMode()">
                                        <g:message code="storage.overwrite.a.key"/>
                                    </h4>
                                </div>

                                <div class="modal-body">
                                    <div class="form-group">
                                        <label for="storageuploadtype" class="col-sm-3 control-label">
                                            <g:message code="storage.key.type.prompt"/>
                                        </label>
                                        <div class="col-sm-9">
                                            <select name="uploadKeyType" id="storageuploadtype" class="form-control" data-bind="value: upload.keyType">
                                                <option value="private"><g:message code="storage.private.key"/></option>
                                                <option value="public"><g:message code="storage.public.key"/></option>
                                                <option value="password"><g:message code="storage.password"/></option>
                                            </select>
                                            <div class="help-block">
                                                <g:message code="storage.keyType.help.text"/>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="form-group" data-bind="css: { 'has-warning': !upload.validInput(), 'has-success': upload.validInput() }">
                                        <div class="col-sm-3" data-bind="visible:  upload.keyType()!='password'">
                                            <select class="form-control" data-bind="value: upload.inputType" name="inputType">
                                                <option value="text"><g:message code="enter.text"/></option>
                                                <option value="file"><g:message code="upload.file"/></option>
                                            </select>
                                        </div>
                                        <label for="uploadpasswordfield" class="col-sm-3 control-label" data-bind="visible:  upload.keyType()=='password'">
                                            <g:message code="enter.text"/>
                                        </label>
                                        <div class="col-sm-9">
                                            <div data-bind="visible: upload.inputType()=='file' ">
                                                <input type="file" name="storagefile" id="storageuploadfile" data-bind="value: upload.file"/>
                                            </div>
                                            <div data-bind="if: (upload.inputType()=='text' || upload.inputType()=='file') && upload.keyType() != 'password'" >
                                                <textarea class="form-control" rows="5" id="storageuploadtext" data-bind="value: upload.textArea, visible: upload.inputType() != 'file'" name="uploadText"></textarea>
                                            </div>
                                            <div data-bind="if: upload.inputType()=='text' && upload.keyType()=='password' ">
                                                <input name="uploadPassword" type="password" placeholder="Enter a password" autocomplete="new-password" data-bind="value: upload.password" id="uploadpasswordfield" class="form-control"/>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="form-group">
                                        <label for="uploadResourcePath2" class="col-sm-3 control-label">
                                            <g:message code="storage.path.prompt"/>
                                        </label>

                                        <div class="col-sm-9">
                                            <div class="input-group">
                                                <div class="input-group-addon" data-bind="if: staticRoot()">
                                                    <span data-bind="text: rootBasePath()"></span>
                                                </div>
                                                <input data-bind="value: inputPath, valueUpdate: 'keyup', attr: { disabled: upload.modifyMode() } " id="uploadResourcePath2" name="userInput" class="form-control" placeholder="Enter the directory name"/>
                                                <input id="uploadResourcePath4" type="hidden" data-bind="value: inputBasePath,  attr: { disabled: upload.modifyMode() } " name="relativePath"/>
                                                <input id="uploadResourcePath3" type="hidden" data-bind="value: inputBasePath,  attr: { disabled: !upload.modifyMode() } " name="relativePath"/>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="form-group" data-bind="css: { 'has-warning': !upload.fileName()&&upload.inputType()!='file', 'has-success': upload.fileName() && upload.inputType()!='file' }">
                                        <label for="uploadResourceName2" class="col-sm-3 control-label">
                                            <g:message code="name.prompt"/>
                                        </label>

                                        <div class="col-sm-9">
                                            <input id="uploadResourceName2" data-bind="value: upload.fileName, valueUpdate: 'keyup', attr: { disabled: upload.modifyMode() } " name="fileName" class="form-control" placeholder="Specify a name."/>
                                            <div class="help-block" data-bind="if: upload.inputType() == 'file'">
                                                <g:message code="storage.upload.file.name.description"/>
                                            </div>
                                            <input id="uploadResourceName3" type="hidden" data-bind="value: upload.fileName,  attr: { disabled: !upload.modifyMode() } " name="fileName"/>
                                        </div>
                                    </div>
                                    <div class="form-group">
                                        <div class=" col-sm-offset-3 col-sm-9">
                                            <div class="checkbox">
                                                <input type="checkbox" value="true" name="dontOverwrite"/>
                                                <label>
                                                    <g:message code="storage.upload.dontOverwrite.label"/>
                                                </label>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="form-group">
                                        <div class="col-sm-12">
                                            <div class="help-block">
                                                <p><g:message code="storage.upload.fullpath.label"/></p>

                                                <p>
                                                    <strong data-bind="text: upload.inputFullpath()" class="text-info"></strong>
                                                </p>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="modal-footer">
                                    <button type="button" class="btn btn-sm btn-default" data-dismiss="modal"><g:message code="cancel"/></button>
                                    <input type="submit" class="btn btn-sm btn-cta obs-storageupload-select" data-bind="attr: { disabled: !upload.validInput() }" value="Save"/>
                                </div>
                            </g:uploadForm>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        </body>
    </feature:disabled>

    <feature:enabled name="vueKeyStorage">
        <body>
        <div class="content">
            <div id="layoutBodyVue">
                <div class="title">
                    <span class="text-h3"><i class="fas fa-key"></i> ${g.message(code:"gui.menu.KeyStorage")}</span>
                </div>
                <div id="keyStoragePage">
                    <key-storage-page project="${params.project}" :read-only="false" :allow-upload="true"></key-storage-page>
                </div>
            </div>
        </div>
        </body>
    </feature:enabled>

