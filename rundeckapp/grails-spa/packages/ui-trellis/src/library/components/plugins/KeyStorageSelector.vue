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
            };
        },
        methods: {
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

