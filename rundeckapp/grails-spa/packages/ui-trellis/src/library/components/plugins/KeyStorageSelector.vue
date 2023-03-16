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
        <modal v-model="modalOpen" :beforeClose="beforeModalClose" title="Select a Storage File" id="storage-file" disabled="readOnly" ref="modalkeys" auto-focus append-to-body cancel-text="Cancel" ok-text="Save">
          <key-storage-view @input="onSelectedKeyChange" :read-only="readOnly" :allow-upload="allowUpload" :value="value" @closeSelector="closeSelector" @closeEditor="closeEditor" @openSelector="openSelector" @openEditor="openEditor"></key-storage-view>
        </modal>

        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body cancel-text="Cancel" ok-text="Save">
          <key-storage-edit :storage-filter="storageFilter" @closeEditor="closeEditor"></key-storage-edit>
        </modal>
    </div>
</template>


<script lang="ts">
    import Vue from 'vue';

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
      components: { KeyStorageEdit, KeyStorageView },
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
              selectedKey: '' as String,
              rootPath: 'keys',
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
        beforeModalClose(args: string | Array<any>) {
          
          if(args === "ok") {
            // Save the selected value - Emit a key changd event with the selected value
            this.$emit("selectedKeyChanged", this.selectedKey)
          } 
          return true
        },
        clean() {            
          this.selectedKey = this.value;
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
        onSelectedKeyChange(selectedKey: String) {
          console.log("==> Selected Key: ", selectedKey)
          this.selectedKey = selectedKey
        }
      }
    });
</script>

