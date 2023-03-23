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
        <modal v-model="modalOpen" :beforeClose="beforeModalClose" title="Select a Storage File" id="storage-file" disabled="readOnly" auto-focus append-to-body cancel-text="Cancel" ok-text="Save">
          <key-storage-view ref="keyStorageViewRef" @input="onSelectedKeyChange" :read-only="readOnly" :allow-upload="allowUpload" :value="value" @closeSelector="closeSelector" @openSelector="openSelector" @openEditor="openEditor"></key-storage-view>
        </modal>
        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" :footer="false" auto-focus append-to-body>
          <key-storage-edit :uploadSetting="uploadSetting" :storage-filter="storageFilter" @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing" ></key-storage-edit>
        </modal>
    </div>
</template>


<script lang="ts">
    import Vue from 'vue';

    import KeyStorageView from "../../components/storage/KeyStorageView.vue"
    import KeyStorageEdit from "../../components/storage/KeyStorageEdit.vue"

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
              uploadSetting: {}
          };
      },
      mounted() {
        console.log("==> initial value: ", this.value)
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
            if(this.selectedKey !== this.value) {
              this.$emit("input", this.selectedKey)
            }
          } 
          return true
        },
        clean() {            
          this.selectedKey = this.value;
          this.inputPath = '';
          this.invalid = false;
          this.errorMsg = '';
        },
        handleFinishEditing(selectedKey: any){
          // @ts-ignore
          this.$refs.keyStorageViewRef.loadKeys(selectedKey);
          this.modalEdit = false
        },
        handleCancelEditing() {
          this.modalEdit = false
        },
        closeSelector(){
          this.modalOpen=false
        },
        openSelector() {
          this.modalOpen = true;
        },
        openEditor(uploadSetting: {}) {
          this.uploadSetting = uploadSetting
          this.modalEdit = true
        },
        onSelectedKeyChange(selectedKey: String) {
          this.selectedKey = selectedKey
        }
      }
    });
</script>

