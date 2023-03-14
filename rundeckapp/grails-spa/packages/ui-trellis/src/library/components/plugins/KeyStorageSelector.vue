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
          <key-storage-view :read-only="readOnly" :allow-upload="allowUpload" :value="value" @closeSelector="closeSelector" @closeEditor="closeEditor" @openSelector="openSelector" @openEditor="openEditor"></key-storage-view>
        </modal>

        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus
               append-to-body cancel-text="Cancel" ok-text="Save">
          <key-storage-edit :storage-filter="storageFilter" @closeEditor="closeEditor"></key-storage-edit>
        </modal>
    </div>

</template>


<script lang="ts">
    import Vue from 'vue';

    import {
      getRundeckContext,
    } from '../../rundeckService';

    import KeyStorageView from "../../components/storage/KeyStorageView.vue"
    import KeyStorageEdit from "../../components/storage/KeyStorageEdit.vue";

    export enum KeyType {
        Public = 'publicKey',
        Private = 'privateKey',
        Password = 'password',
    }

    export enum InputType {
        Text = 'text',
        File = 'file',
    }

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
                selectedKey: {} as any,
                isSelectedKey: false,
                path: '',
                upPath: '',
                upload: {} as any,
                rootPath: 'keys',
                staticRoot: true,
                directories: [] as any,
                files: [] as any,
                inputPath: '',
                invalid: false,
                errorMsg: '',
                uploadErrors: {} as any
            };
        },
        methods: {
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
            closeEditor(){
              this.modalEdit=false
            },
            closeSelector(){
              this.modalOpen=false
            },
            openSelector() {
              this.modalOpen = true;
            },
            openEditor() {
              this.modalEdit = true
            },
            parentDirString(path: any) {
              if (null != path && path.lastIndexOf('/') >= 0) {
                return path.substring(0, path.lastIndexOf('/'));
              } else {
                return '';
              }
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

          },
          setRootPath() {
            if (this.rootPath == null || this.rootPath === '') {
              this.rootPath = 'keys';
            }
            this.path = '';
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
          absolutePath(relpath: any) {
            var root = this.rootPath;
            var statroot = this.staticRoot;
            if (statroot === false) {
              return relpath;
            }
            return root + '/' + relpath;
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
          relativePath(path: any) {
            const root = this.rootPath;
            const statroot = this.staticRoot;
            if (!statroot) {
              return path;
            }
            let newpath = '';
            if (path != null && root != null) {
              path = this.cleanPath(path);
              newpath = this.cleanPath(path.substring(root.length));
            }
            return newpath;
          },
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
        }
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

