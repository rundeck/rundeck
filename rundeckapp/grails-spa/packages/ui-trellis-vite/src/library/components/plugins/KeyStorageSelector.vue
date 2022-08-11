<!--
  - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
  -->

<template>
    <div>

        <btn class="btn btn-sm btn-default" @click="openSelector()">
            <slot>Select… <i class="glyphicon glyphicon-folder-open"></i></slot>
        </btn>

        <modal v-model="modalOpen" title="Select a Storage File" id="storage-file" ref="modalkeys" auto-focus
               append-to-body>

            <div class="alert alert-warning" v-if="errorMsg!=''">
                <span>{{errorMsg}}</span>
            </div>

            <div class="row text-info ">
                <div class="form-group col-sm-12" :class="[invalid===true ? 'has-error' : '']">
                    <div class="input-group">
                        <div class="input-group-addon" v-if="staticRoot">
                            <span>{{showRootPath}}</span>
                        </div>
                        <input type="text" class="form-control" style="padding-left:18px"
                               v-model="inputPath" @keyup.enter="loadDirInputPath()"
                               placeholder="Enter a path"/>
                    </div>
                </div>
            </div>
            <div class="row keySelector">
                <div class="col-sm-12">
                    <div class="keySelector-button-group" style="margin-bottom:1em;">
                        <button type="button" class="btn btn-sm btn-default"
                                @click="loadDir(upPath)" :disabled="upPath===''">
                            <i class="glyphicon glyphicon-folder-open"></i>
                            <i class="glyphicon glyphicon-arrow-up"></i>
                            <span>{{showUpPath()}}</span>
                        </button>

                        <button @click="actionUpload()" class="btn btn-sm btn-cta" v-if="this.allowUpload===true">
                            <i class="glyphicon glyphicon-plus"></i>
                            Add or Upload a Keys
                        </button>

                        <button @click="actionUploadModify()" class="btn btn-sm btn-info "
                                v-if="this.allowUpload===true && this.isSelectedKey===true">
                            <i class="glyphicon glyphicon-pencil"></i>
                            Overwrite Key
                        </button>
                    </div>


                    <table class="table table-hover table-condensed">
                        <tbody>
                        <tr>
                            <td colspan="2" class="text-strong">
                                <span v-if="files.length<1">
                                  No keys
                                </span>
                                <span v-if="files.length>0">
                                  <span>{{files.length}}</span>
                                  keys
                                </span>
                            </td>
                        </tr>
                        </tbody>
                        <tbody>
                        <tr v-for="key in files" :class="[key.path== selectedKey.path ? selectedClass : '','action']"
                            :key="key.name" @click="selectKey(key)">
                            <td>
                                <i :class="[key.path== selectedKey.path ? 'glyphicon glyphicon-ok' :'glyphicon glyphicon-unchecked']"></i>

                                <span v-if="isPrivateKey(key)"
                                      title="This path contains a private key that can be used for remote node execution.">
                                  <i class="glyphicon glyphicon-lock"></i>
                                </span>
                                <span v-if="isPublicKey(key)">
                                  <i class="glyphicon glyphicon-eye-open"></i>
                                </span>
                                <span v-if="isPassword(key)"
                                      title="This path contains a password that can be used for remote node execution.">
                                  <i class="glyphicon glyphicon-lock"></i>
                                </span>

                                <span>{{key.name}}</span>
                            </td>
                            <td class="text-strong">
                                <span class="pull-right">
                                  <span v-if="isPrivateKey(key)"
                                        title="This path contains a private key that can be used for remote node execution.">
                                    Private Key
                                  </span>
                                  <span v-if="isPublicKey(key)">
                                    Public Key
                                  </span>
                                  <span v-if="isPassword(key)"
                                        title="This path contains a password that can be used for remote node execution.">
                                    Password
                                  </span>
                                </span>
                            </td>
                        </tr>
                        </tbody>

                        <tbody v-if="notFound()===true">
                        <tr>
                            <td colspan="2">
                                <span class="text-strong">Nothing found at this path.
                                </span>
                            </td>
                        </tr>
                        </tbody>
                        <tbody>
                        <tr v-for="directory in directories" :key="directory.name">
                            <td class="action" @click="loadDir(directory.path)" colspan="2">
                                <i class="glyphicon glyphicon-arrow-down"></i>
                                <i class="glyphicon glyphicon-folder-close"></i>
                                <span>{{dirNameString(directory.path)}}</span>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="row" v-if="isKeySelected()===true">
                <div class="col-sm-12">
                    <div class="well">
                        <div>
                            Storage path:
                            <code class="text-success">{{selectedKey.path}}</code>
                            <a href="#" data-bind="attr: { href: selectedPathUrl() }">
                                <i class="glyphicon glyphicon-link"></i>
                            </a>
                        </div>
                        <div v-if="createdTime()!==''">
                            <div>
                                Created:
                                <span class="timeabs text-strong">
                                    {{createdTime() | moment("dddd, MMMM Do YYYY, h:mm:ss a") }}
                                </span>

                                <span v-if="createdUsername()!==''">
                                    by:
                                    <span class="text-strong">{{createdUsername()}}</span>
                                </span>

                            </div>
                        </div>
                        <div v-if="wasModified()!==''">
                            <div>
                                Modified:
                                <span class="timeago text-strong">
                                    {{modifiedTimeAgoText()| duration('humanize') }} ago
                                </span>

                                <span v-if="modifiedUsername()!==''">
                                by:
                                <span class="text-strong">{{modifiedUsername()}}</span>
                              </span>
                            </div>
                        </div>
                    </div>

                </div>
            </div>


            <div slot="footer">
                <button type="button" class="btn btn-sm btn-default" @click="handleCancel">Cancel</button>
                <button type="button" class="btn btn-sm btn-success obs-storagebrowse-select"
                        :disabled="isKeySelected()===false" @click="handleSelectKey">
                    Choose Selected Key
                </button>
            </div>

        </modal>

        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus
               append-to-body cancel-text="Cancel" ok-text="Save">

            <div class="alert alert-danger" v-if="upload.errorMsg!==null">
                <span>{{upload.errorMsg}}</span>
            </div>

            <div class="row">
                <div class="col-md-12">
                    <div class="form-group row text-right">
                        <label for="storageuploadtype" class="col-sm-3 control-label label-key">
                            Key Type:
                        </label>
                        <div class="col-sm-9">
                            <select name="uploadKeyType" class="form-control" v-model="upload.keyType">
                                <option v-for="option in keyTypes" v-bind:value="option.value">
                                    {{ option.text }}
                                </option>
                            </select>
                            <div class="help-block text-left">
                                Private Keys and Passwords are not available to download once they are stored. Public
                                keys can be downloaded.
                            </div>
                        </div>
                    </div>

                    <div class="form-group row text-right" :class="[validInput()==true?'has-success':'has-warning']">
                        <div class="col-sm-3 label-key" v-if="upload.keyType!=='password'">
                            <select class="form-control" name="inputType" v-model="upload.inputType">
                                <option v-for="option in inputTypes" v-bind:value="option.value">
                                    {{ option.text }}
                                </option>
                            </select>
                        </div>
                        <label for="uploadpasswordfield" class="col-sm-3 control-label label-key"
                               v-if="upload.keyType==='password'">
                            Enter text
                        </label>
                        <div class="col-sm-9">

                            <div v-if="upload.inputType=='text' && upload.keyType!='password'">
                                <textarea class="form-control" rows="5" id="storageuploadtext" v-model="upload.textArea"
                                          name="uploadText"></textarea>
                            </div>

                            <div v-if="upload.inputType=='file' ">
                                <input type="file" id="file" ref="file" @change="handleFileUpload"/>

                            </div>

                            <div v-if="upload.inputType=='text' && upload.keyType=='password' ">
                                <input name="uploadPassword" type="password" placeholder="Enter a password"
                                       autocomplete="new-password" v-model="upload.password" id="uploadpasswordfield"
                                       class="form-control"/>
                            </div>
                        </div>
                    </div>

                    <div class="form-group row text-right">
                        <label for="uploadResourcePath2" class="col-sm-3 control-label label-key">
                            Storage path:
                        </label>

                        <div class="col-sm-9">
                            <div class="input-group">
                                <div class="input-group-addon">
                                    <span>{{showRootPath}}</span>
                                </div>
                                <input v-model="upload.inputPath" :disabled="upload.modifyMode==true"
                                       id="uploadResourcePath2" name="relativePath" class="form-control"
                                       placeholder="Enter the directory name"/>
                                <input v-model="upload.inputPath" :disabled="upload.modifyMode==false"
                                       id="uploadResourcePath3" type="hidden" name="relativePath"/>
                            </div>
                        </div>
                    </div>

                    <div :class="['form-group','row',upload.fileName==null&&upload.inputType!='file'?'has-warning':'', upload.fileName!=null&&upload.inputType!='file'?'has-success':'']">
                        <label for="uploadResourceName2" class="col-sm-3 control-label label-key text-right">
                            Name:
                        </label>

                        <div class="col-sm-9">
                            <input id="uploadResourceName2" v-model="upload.fileName"
                                   :disabled="upload.modifyMode==true" name="fileName" class="form-control"
                                   placeholder="Specify a name."/>
                            <div class="help-block" v-if="upload.inputType == 'file'">
                                If not set, the name of the uploaded file is used.
                            </div>
                            <input id="uploadResourceName3" type="hidden" v-model="upload.fileName"
                                   :disabled="upload.modifyMode==false" name="fileName"/>
                        </div>
                    </div>
                    <div class="form-group row">
                        <div class=" col-sm-offset-3 col-sm-9">
                            <div class="checkbox">
                                <input type="checkbox" value="true" name="dontOverwrite"
                                       v-model="upload.dontOverwrite"/>
                                <label>
                                    Do not overwrite a file with the same name.
                                </label>
                            </div>
                        </div>
                    </div>

                    <div class="form-group row">
                        <div class="col-sm-12">
                            <div class="help-block">
                                <p>You can reference this stored Key using the storage path:</p>

                                <p>
                                    <strong class="text-info">{{uploadFullPath}}</strong>
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div slot="footer">
                <button type="button" class="btn btn-sm btn-default" @click="modalEdit=false">Cancel</button>
                <button type="button" class="btn btn-sm btn-success" :disabled="validInput()===false"
                        @click="handleUploadKey">
                    Save
                </button>
            </div>

        </modal>
    </div>

</template>


<script lang="ts">
    import Vue from 'vue';

    import {
        getRundeckContext,
    } from '../../rundeckService';

    import moment from 'moment';

    export enum KeyType {
        Public = 'publicKey',
        Private = 'privateKey',
        Password = 'password',
    };

    export enum InputType {
        Text = 'text',
        File = 'file',
    };

    export default Vue.extend({
        name: 'KeyStorageSelector',
        props: [
            'value',
            'storageFilter',
            'allowUpload'
        ],
        data() {
            return {
                modalOpen: false,
                modalEdit: false,
                inputPath: '',
                selectedKey: {} as any,
                isSelectedKey: false,
                files: [] as any,
                directories: [] as any,
                selectedClass: 'success',
                errorMsg: '',
                staticRoot: true,
                path: '',
                rootPath: 'keys',
                upPath: '',
                invalid: false,
                keyTypes: [
                    {text: 'Private Key', value: 'private'},
                    {text: 'Public Key', value: 'public'},
                    {text: 'Password', value: 'password'},
                ],
                inputTypes: [
                    {text: 'Enter text', value: 'text'},
                    {text: 'Upload File', value: 'file'},
                ],
                upload: {} as any,
                uploadErrors: {} as any,
            };
        },
        methods: {
            openSelector() {
                this.invalid = false;
                this.setRootPath();
                this.clean();
                if (this.value != null) {
                    const parentDir = this.parentDirString(this.value)
                    this.loadDir(parentDir);
                    this.loadUpPath();
                    this.defaultSelectKey(this.value);
                }
                this.modalOpen = true;
            },
            setRootPath() {
                if (this.rootPath == null || this.rootPath === '') {
                    this.rootPath = 'keys';
                }
                this.path = '';
            },
            selectKey(key: any) {
                if (this.selectedKey != null && this.selectedKey.path === key.path) {
                    this.selectedKey = {};
                    this.isSelectedKey = false;
                } else {
                    this.selectedKey = key;
                    this.isSelectedKey = true;
                }
            },
            isKeySelected() {
                if (this.selectedKey.path != null) {
                    return true
                }
                return false
            },
            handleSelectKey() {
                this.modalOpen = false;
                this.$emit('input', this.selectedKey ? this.selectedKey.path : '');
                this.clean();
            },
            handleCancel() {
                this.clean();
                this.modalOpen = false;
            },
            loadUpPath() {
                let upPath = '';
                if (this.path != '' && this.path != this.rootPath && this.path != this.rootPath + '/') {
                    if (this.path.indexOf('/') >= 0) {
                        upPath = this.parentDirString(this.path);
                    } else {
                        upPath = this.rootPath;
                    }

                    if (upPath != this.rootPath) {
                        this.upPath = this.absolutePath(upPath);
                    } else {
                        this.upPath = this.rootPath;
                    }
                } else {
                    this.upPath = upPath;
                }
            },
            showUpPath() {
                if (this.upPath != this.rootPath) {
                    return this.relativePath(this.upPath);
                } else {
                    return this.upPath;
                }
            },
            checkParentDir(path: any) {
                const rundeckContext = getRundeckContext();
                const fullPath = this.absolutePath(path);

                rundeckContext.rundeckClient.storageKeyGetMetadata(path).then((result: any) => {
                    if (result.resources != null) {
                        const keys = result.resources.filter((resource: any) => resource.path.indexOf(fullPath) >= 0);
                        if (keys.length == 0) {
                            this.invalid = true
                            this.errorMsg = 'invalid path';
                        }
                    } else {
                        this.invalid = true
                        this.errorMsg = 'invalid path';
                    }
                }).catch((err) => {
                    this.invalid = true
                    this.errorMsg = 'invalid path';
                });
            },
            loadDir(selectedPath: any) {
                this.clean();
                let path = '';
                if (selectedPath != null) {
                    path = this.relativePath(selectedPath);
                }

                this.path = path;
                this.inputPath = path;

                this.checkParentDir(path)

                this.loadUpPath();
                this.loadKeys();
            },
            loadDirInputPath() {
                this.clean()
                this.loadDir(this.inputPath)
            },
            loadKeys() {
                const rundeckContext = getRundeckContext();
                rundeckContext.rundeckClient.storageKeyGetMetadata(this.path).then((result: any) => {
                    this.directories = [];
                    this.files = [];

                    if (result.resources != null) {
                        result.resources.forEach((resource: any) => {
                            if (resource.type === 'directory') {
                                this.directories.push(resource);

                                this.directories.sort((obj1: any, obj2: any) => {
                                    if (obj1.path > obj2.path) {
                                        return 1;
                                    }

                                    if (obj1.path < obj2.path) {
                                        return -1;
                                    }
                                    return 0;
                                });
                            }

                            if (resource.type === 'file') {
                                if (this.storageFilter != null) {
                                    if (this.allowedResource(resource.meta)) {
                                        this.files.push(resource);
                                    }
                                } else {
                                    this.files.push(resource);
                                }

                                this.files.sort((obj1: any, obj2: any) => {
                                    if (obj1.path > obj2.path) {
                                        return 1;
                                    }

                                    if (obj1.path < obj2.path) {
                                        return -1;
                                    }
                                    return 0;
                                });
                            }
                        });
                    }
                }).catch((err) => {
                    this.errorMsg = err;
                });
            },
            allowedResource(meta: any) {
                const filterArray = this.storageFilter.split('=');
                const key = filterArray[0];
                const value = filterArray[1];

                if (key == 'Rundeck-key-type') {
                    if (value === meta['rundeckKeyType']) {
                        return true;
                    }
                } else {
                    if (key == 'Rundeck-data-type') {
                        if (value === meta['Rundeck-data-type']) {
                            return true;
                        }
                    }
                }
                return false;
            },
            defaultSelectKey(path: any) {
                const rundeckContext = getRundeckContext();

                rundeckContext.rundeckClient.storageKeyGetMetadata(this.path).then((result: any) => {
                    if (result.resources != null) {
                        result.resources.forEach((resource: any) => {
                            if (resource.type === 'file') {
                                if (resource.path === path) {
                                    this.selectedKey = resource;
                                    this.isSelectedKey = true;
                                }
                            }
                        });
                    }
                });

            },
            dirNameString(path: any) {
                if (path.lastIndexOf('/') >= 0) {
                    return path.substring(path.lastIndexOf('/') + 1);
                } else {
                    return path;
                }
            },
            isPrivateKey(key: any) {
                if (key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'private') {
                    return true;
                }
                return false;
            },
            isPublicKey(key: any) {
                if (key.meta['rundeckKeyType'] && key.meta["rundeckKeyType"] === 'public') {
                    return true;
                }
                return false;
            },
            isPassword(key: any) {
                if (key.meta["Rundeck-data-type"] === 'password') {
                    return true;
                }
                return false;
            },
            notFound() {
                return false;
            },
            clean() {
                this.directories = [];
                this.files = [];
                this.selectedKey = {};
                this.isSelectedKey = false;
                this.inputPath = '';
                this.invalid = false;
                this.errorMsg = '';
                this.uploadErrors = {} as any;
            },
            cleanPath(path: any) {
                if (path != null) {
                    while (path.indexOf('/') == 0) {
                        path = path.substring(1);
                    }
                } else {
                    return '';
                }
                return path;
            },
            relativePath(path: any) {
                var root = this.rootPath;
                var statroot = this.staticRoot;
                if (!statroot) {
                    return path;
                }
                var newpath = '';
                if (path != null && root != null) {
                    path = this.cleanPath(path);
                    newpath = this.cleanPath(path.substring(root.length));
                }
                return newpath;
            },
            parentDirString(path: any) {
                if (null != path && path.lastIndexOf('/') >= 0) {
                    return path.substring(0, path.lastIndexOf('/'));
                } else {
                    return '';
                }
            },
            absolutePath(relpath: any) {
                var root = this.rootPath;
                var statroot = this.staticRoot;

                if (statroot === false) {
                    return relpath;
                }
                return root + '/' + relpath;
            },
            wasModified() {
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-content-creation-time'] != null &&
                    this.selectedKey.meta['Rundeck-content-modify-time'] != null) {
                    return this.selectedKey.meta['Rundeck-content-creation-time'] != this.selectedKey.meta['Rundeck-content-modify-time'];
                }
                return false;
            },
            modifiedUsername() {
                var value = '';
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-auth-modified-username']) {
                    return this.selectedKey.meta['Rundeck-auth-modified-username'];
                }
                return value;
            },
            createdUsername() {
                let value = '';
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-auth-created-username'] != null) {
                    return this.selectedKey.meta['Rundeck-auth-created-username'];
                }
                return value;
            },
            createdTime() {
                var value = '';
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-content-creation-time'] != null) {
                    value = this.selectedKey.meta['Rundeck-content-creation-time'];
                }
                return value;
            },
            modifiedTime() {
                var value = '';
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-content-modify-time'] != null) {
                    value = this.selectedKey.meta['Rundeck-content-modify-time'];
                }
                return value;
            },
            modifiedTimeAgoText() {
                var value = 0;
                if (this.selectedKey != null &&
                    this.selectedKey.meta != null &&
                    this.selectedKey.meta['Rundeck-content-modify-time'] != null) {
                    var time = this.selectedKey.meta['Rundeck-content-modify-time'];
                    value = this.duration(time);
                }
                return value;
            },
            duration(start: any) {
                return moment().diff(moment(start));
            },
            actionUpload() {
                this.modalEdit = true;
                let type = 'private';
                let inputType = InputType.Text;

                this.upload = {
                    modifyMode: false,
                    keyType: type,
                    inputPath: this.inputPath,
                    inputType: inputType,
                    fileName: null,
                    file: '',
                    fileContent: '',
                    textArea: '',
                    password: '',
                    status: 'new',
                    errorMsg: null as any,
                };
            },
            actionUploadModify() {
                const isPassword = this.isPassword(this.selectedKey);
                const isPrivateKey = this.isPrivateKey(this.selectedKey);
                const isPublicKey = this.isPublicKey(this.selectedKey);

                let type = '';
                if (isPassword) {
                    type = 'password';
                }
                if (isPrivateKey) {
                    type = 'private';
                }
                if (isPublicKey) {
                    type = 'public';
                }

                const inputPath = this.relativePath(this.parentDirString(this.selectedKey.path));

                let inputType = InputType.File;
                if (isPassword) {
                    inputType = InputType.Text;
                }

                this.upload = {
                    modifyMode: true,
                    keyType: type,
                    inputPath: inputPath,
                    inputType: inputType,
                    fileName: this.selectedKey.name,
                    file: '',
                    fileContent: '',
                    textArea: '',
                    password: '',
                    status: 'update',
                    errorMsg: null as any,
                };
                this.modalEdit = true;
            },
            validInput() {
                var intype = this.upload.inputType;
                var file = this.upload.file;
                var textarea = this.upload.textArea;
                var pass = this.upload.password;
                if (intype == 'text') {
                    return (textarea || pass) ? true : false;
                } else {
                    return file ? true : false;
                }
            },
            async handleUploadKey() {
                const rundeckContext = getRundeckContext();

                let fullPath = this.getKeyPath();

                let type = KeyType.Public;
                let contentType = 'application/pgp-keys';

                let value = null as any;

                switch (this.upload.keyType) {
                    case 'password':
                        type = KeyType.Password;
                        contentType = 'application/x-rundeck-data-password';
                        value = this.upload.password;
                        break;
                    case 'private':
                        type = KeyType.Private;
                        contentType = 'application/octet-stream';

                        if (this.upload.inputType === InputType.Text) {
                            value = this.upload.textArea;
                        } else {
                            if(this.upload.fileContent ==''){
                                this.upload.errorMsg = 'File content was not read';
                                this.upload.file = null;
                            }else{
                                value = this.upload.fileContent;
                            }
                        }
                        break;
                    case 'public':
                        if (this.upload.inputType === InputType.Text) {
                            value = this.upload.textArea;
                        } else {
                            if(this.upload.fileContent ==''){
                                this.upload.errorMsg = 'File content was not read';
                                this.upload.file = null;
                            }else{
                                value = this.upload.fileContent;
                            }
                        }
                        break;
                }

                let saved = true;

                if (this.upload.status == 'new') {

                    const checkKey = await rundeckContext.rundeckClient.storageKeyGetMaterial(fullPath);
                    let exists = true;
                    if(checkKey._response.status==404){
                        exists = false;
                    }

                    if(exists && this.upload.dontOverwrite ) {
                        this.upload.errorMsg = 'key aready exists';
                        saved = false;
                    }else{
                        if(!exists) {
                            const resp = await rundeckContext.rundeckClient.storageKeyCreate(fullPath, value, {contentType});
                            if(resp._response.status!=201){
                                saved = false;
                                this.upload.errorMsg = resp.error;
                            }
                        }else {
                            this.upload.status = 'update';
                        }
                    }
                }

                if (this.upload.status === 'update') {
                    const resp = await rundeckContext.rundeckClient.storageKeyUpdate(fullPath, value, {contentType});

                    if(resp._response.status!=201){
                        saved = false;
                        this.upload.errorMsg = resp.error;
                    }

                    saved = true;
                }

                if(saved){
                    this.loadKeys();
                    this.modalEdit = false;
                }
            },
            handleFileUpload(e: any){
                var files = e.target.files || e.dataTransfer.files;
                if (!files.length)
                    return;

                const file = files[0];
                this.upload.file = file.name;

                let reader = new FileReader();
                reader.onload = (event:any) => {
                    let text = event.target.result;
                    this.upload.fileContent = text;
                    if(this.upload.errorMsg!=null){
                        this.upload.errorMsg = null;
                    }
                };
                reader.onerror = (event:any) => {
                    this.upload.errorMsg = "file cannot be read";
                    this.upload.file = null;
                };
                reader.readAsText(file);

            },
            getKeyPath(){
                let fullPath = this.upload.inputPath!=null && this.upload.inputPath!=''? this.upload.inputPath + '/':'';

                if(this.upload.fileName != null) {
                    fullPath = fullPath + this.upload.fileName;
                }else{
                    if(this.upload.file!=null){
                        fullPath = fullPath + this.upload.file;
                    }
                }

                return fullPath;

            }
        },
        computed: {
            showRootPath: function () {
                return "keys/"
            },
            uploadFullPath(): string {
                return 'keys/' + this.getKeyPath();
            },
        },
        watch: {
            value(newValue, oldValue) {
                this.setRootPath();
                this.clean();

                if (this.value != null) {
                    this.defaultSelectKey(newValue);
                    this.loadDir(this.parentDirString(this.value));
                }

                this.$emit('input', newValue);
            },
        },
    });
</script>

<style>
    .keySelector span {
        content: " ";
        margin: 0 2px;
    }

    .keySelector i {
        content: " ";
        margin: 0 1px;
    }

    .keySelector-button-group button {
        content: " ";
        margin: 0 2px;
    }

    label-key {
        vertical-align: middle
    }
</style>

