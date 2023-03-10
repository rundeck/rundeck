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
      <key-storage-browser>
      </key-storage-browser>
    </div>

</template>


<script lang="ts">
    import Vue from 'vue';
    import KeyStorageBrowser from "../../../app/components/storage/KeyStorageBrowser.vue";

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

