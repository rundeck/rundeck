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

        <modal v-model="modalOpen" title="Select a Storage File" id="storage-file" disabled="readOnly" ref="modalkeys" auto-focus
               append-to-body>
          <key-storage-view :read-only="readOnly" :allow-upload="allowUpload" :value="value" @cancelled="cancelled" @handled="handled"></key-storage-view>
        </modal>

        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus
               append-to-body cancel-text="Cancel" ok-text="Save">
          <key-storage-edit :storage-filter="storageFilter" @saved="saved" @editCancelled="editCancelled"></key-storage-edit>

        </modal>
    </div>

</template>


<script lang="ts">
    import Vue from 'vue';

    import {
        getRundeckContext,
    } from '../../rundeckService';

    import moment from 'moment';
    import KeyStorageView from "../../../app/components/storage/KeyStorageView.vue";
    import KeyStorageEdit from "../../../app/components/storage/KeyStorageEdit.vue";

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
      components: {KeyStorageEdit, KeyStorageView},
      props: [
            'value',
            'storageFilter',
            'allowUpload',
            'readOnly'
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
            saved(){
              this.modalEdit=false
            },
            handled(){
              this.modalEdit=false
            },
            cancelled(){
              this.modalOpen=false
            },
            editCancelled(){
              this.modalEdit=false
            },
            openSelector() {
                if(this.readOnly!==true) {
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
                }
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

