    %{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

<g:jsonToken id="storage_browser_token" url="${request.forwardURI}"/>
    <div class="alert alert-warning" data-bind="visible: errorMsg()">
        <span data-bind="text: errorMsg"></span>
    </div>
    <div class="row text-info ">
        <div class="form-group col-sm-12" data-bind="css: invalid()?'has-error':'' ">
            <div class="input-group">
                <div class="input-group-addon" data-bind="if: staticRoot()">
                    <span data-bind="text: rootPath() + '/'"></span>
                </div>
                <input type="text" class="form-control"
                       data-bind="value: inputPath, valueUpdate: 'input', attr: {disabled: loading() }, executeOnEnter: browseToInputPath"
                       placeholder="Enter a path"/>
            </div>
        </div>
    </div>
    <div class="row row-space">
        <div class="col-sm-12">
    <div >
        <button type="button" class="btn btn-sm btn-default"
                data-bind="click: function(){$root.loadDir(upPath())}, css: {disabled: ( !upPath() || invalid() ) }" >
            <i class="glyphicon glyphicon-folder-open"></i>
            <i class="glyphicon glyphicon-arrow-up"></i>
            <span data-bind="text: upPath() ? $root.dirName(upPath()) : '' "></span>
        </button>

        <div class="btn-group" data-bind="if: browseMode()=='browse'">
            <button type="button" class="btn btn-sm btn-default dropdown-toggle"
                    data-bind="css: { disabled: !selectedPath() }"
                    data-toggle="dropdown">
                <g:message code="button.Action" /> <span class="caret"></span>
            </button>
            <ul class="dropdown-menu" role="menu">
                <li><a href="#storageconfirmdelete" data-toggle="modal"><i class="glyphicon glyphicon-remove"></i> <g:message code="button.delete.selected.item" /></a></li>

                <li class="" data-bind=" if: selectedIsDownloadable()">
                    <a href="#" data-bind="click: download"><i class="glyphicon glyphicon-download"></i> <g:message code="button.download.contents" /></a>
                </li>
            </ul>
        </div>

        <div class="btn-group" data-bind="if: allowUpload() ">
            <a href="#storageuploadkey" data-bind="click: actionUpload"
               class="btn btn-sm btn-success ">
                <i class="glyphicon glyphicon-plus"></i> <g:message code="storage.add.or.upload.a.key" />
            </a>
        </div>
        <div class="btn-group" data-bind="if: allowUpload() && selectedPath() ">
            <a href="#storageuploadkey" data-bind="click: actionUploadModify"
               class="btn btn-sm btn-info ">
                <i class="glyphicon glyphicon-pencil"></i> <g:message code="storage.overwrite.key" />
            </a>
        </div>
    </div>
    <div class="loading-area text-info " data-bind="visible: loading()"
         style="width: 100%; height: 200px; padding: 50px; background-color: #eee;">
        <i class="glyphicon glyphicon-time"></i> <g:message code="loading.text" />
    </div>
    <table class="table table-hover table-condensed" data-bind="if: !invalid() && !loading()">
        <tbody data-bind="if: !notFound()">
        <tr>
            <td colspan="2" class="text-muted">
                <span data-bind="if: filteredFiles().length < 1">
                    <g:message code="storage.no.keys" />
                </span>
                <span data-bind="if: filteredFiles().length > 0">
                    <span data-bind="text: filteredFiles().length"></span> <g:message code="storage.keys.plural" />
                </span>
            </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: filteredFiles()">
        <tr data-bind="click: $root.selectFile, css: $root.selectedPath()==path() ? 'success' : '' " class="action">
            <td >
                <i class="glyphicon "
                   data-bind="css: $root.selectedPath()==path() ? 'glyphicon-ok' : 'glyphicon-unchecked' "></i>

                <span data-bind="if: $data.isPrivateKey()"
                      title="This path contains a private key that can be used for remote node execution.">
                    <i class="glyphicon glyphicon-lock"></i>
                </span>
                <span data-bind="if: $data.isPublicKey()">
                    <i class="glyphicon glyphicon-eye-open"></i>
                </span>
                <span data-bind="if: $data.isPassword()"
                      title="This path contains a password that can be used for remote node execution.">
                    <i class="glyphicon glyphicon-lock"></i>
                </span>

                <span data-bind="text: name"></span>
            </td>
            <td class="text-muted">
                <span class="pull-right">
                <span data-bind="if: $data.isPrivateKey()"
                    title="${g.enc(code: 'storage.private.key.description')}"
                >
                    <g:message code="storage.private.key"/>
                </span>
                <span data-bind="if: $data.isPublicKey()">
                    <g:message code="storage.public.key"/>
                </span>
                <span data-bind="if: $data.isPassword()"
                    title="${g.enc(code: 'storage.password.description')}">
                    <g:message code="storage.password"/>
                </span>
                </span>
            </td>
        </tr>
        </tbody>

        <tbody data-bind="if: notFound()">
        <tr>
            <td colspan="2">
                <span class="text-muted"><g:message code="storage.nothing.found.at.this.path" />
                <span data-bind="if: allowUpload()"><g:message code="storage.nothing.found.prompt" /></span>
                </span>
            </td>
        </tr>
        </tbody>
        <tbody data-bind="foreach: directories()">
        <tr>
            <td class="action" data-bind="click: $root.loadDir" colspan="2">
                <i class="glyphicon glyphicon-arrow-down"></i>
                <i class="glyphicon glyphicon-folder-close"></i>
                <span data-bind="text: $root.dirName($data)"></span>
            </td>
        </tr>
        </tbody>

    </table>

        </div>
    </div>
    <div class="row row-space" data-bind="if: selectedPath()">
    <div class="col-sm-12">
        <div class="well">
            <div>
                <g:message code="storage.path.prompt" />
                <code class="text-success"
                      data-bind="text: selectedPath()"></code>
                <a href="#" data-bind="attr: { href: selectedPathUrl() }"><i class="glyphicon glyphicon-link"></i></a>
            </div>

            <div data-bind="if: selectedResource() && selectedResource().createdTime()"
                class="text-muted">
                <div >
                    <g:message code="created.prompt" />
                    <span class="timeabs" data-bind="text: selectedResource().createdTime(), attr: { title:  selectedResource().meta()['Rundeck-content-creation-time'] }"></span>

                    <span data-bind="if: selectedResource().createdUsername()">
                        <g:message code="by.prompt" />

                        <span data-bind="text: selectedResource().createdUsername()"></span>
                    </span>

                </div>
            </div>
            <div data-bind="if: selectedResource() && selectedResource().wasModified()"
                 class="text-muted">
                <div >
                    <g:message code="modified.prompt" />
                    <span class="timeago" data-bind="text: selectedResource().modifiedTimeAgo('ago'), attr: { title:  selectedResource().meta()['Rundeck-content-modify-time'] }"></span>

                    <span data-bind="if: selectedResource().modifiedUsername()">
                        <g:message code="by.prompt"/>

                        <span data-bind="text: selectedResource().modifiedUsername()"></span>
                    </span>
                </div>
            </div>

            <div data-bind="if: selectedResource() && selectedResource().isPublicKey()">
                    <button
                            data-bind="click: function(){$root.actionLoadContents('publicKeyContents',$element);}, visible: !selectedResource().wasDownloaded()"
                            class="btn btn-sm btn-default"
                            data-loading-text="${g.enc(code:"loading")}"
                    >
                        <g:message code="button.view.public.key.contents" /> (<span data-bind="text: selectedResource().contentSize()"></span> <g:message code="bytes" />)
                    </button>

                <div class="pre-scrollable" data-bind="visible: selectedResource().downloadError()">
                    <span data-bind="text:selectedResource().downloadError()" class="text-danger"></span>
                </div>
                <pre id="publicKeyContents"  class="pre-scrollable" data-bind="visible: selectedResource().wasDownloaded()">

                </pre>
            </div>
        </div>


    </div>
    </div>
