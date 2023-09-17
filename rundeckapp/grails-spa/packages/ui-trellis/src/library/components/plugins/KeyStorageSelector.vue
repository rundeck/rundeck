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
          <key-storage-view ref="keyStorageViewRef" @update:modelValue="onSelectedKeyChange" :read-only="readOnly" :root-path="rootPath" :allow-upload="allowUpload" :model-value="modelValue" @closeSelector="closeSelector" @openSelector="openSelector" @openEditor="openEditor"></key-storage-view>
        </modal>
        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" :footer="false" auto-focus append-to-body>
          <key-storage-edit :uploadSetting="uploadSetting" :root-path="rootPath" :storage-filter="storageFilter" @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing" ></key-storage-edit>
        </modal>
    </div>
</template>


<script lang="ts">
import {defineComponent} from 'vue';

    import KeyStorageView from "../../components/storage/KeyStorageView.vue"
    import KeyStorageEdit from "../../components/storage/KeyStorageEdit.vue"
import {UploadSetting} from "../storage/KeyStorageEdit.vue"

    export default defineComponent({
      name: 'KeyStorageSelector',
      components: { KeyStorageEdit, KeyStorageView },
      props: [
        'modelValue',
        'storageFilter',
        'allowUpload',
        'readOnly'
      ],
      emits: ['update:modelValue'],
      data() {
          return {
              modalOpen: false,
              modalEdit: false,
              selectedKey: '' as String,
              rootPath: 'keys',
              inputPath: '',
              invalid: false,
              errorMsg: '',
              uploadSetting: {} as UploadSetting
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
            if (key === 'Rundeck-key-type') {
                if (value === meta['rundeckKeyType']) {
                    return true;
                }
            } else {
                if (key === 'Rundeck-data-type') {
                    if (value === meta['Rundeck-data-type']) {
                        return true;
                    }
                }
            }
            return false;
        },
        beforeModalClose(args: string | Array<any>) {
          if(args === "ok") {
            // Save the selected value - Emit a key changed event with the selected value
            if(this.selectedKey !== this.modelValue) {
              this.$emit("update:modelValue", this.selectedKey)
            }
          }
          return true
        },
        clean() {
          this.selectedKey = this.modelValue;
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
        openEditor(uploadSetting: UploadSetting) {
          this.uploadSetting = uploadSetting
          this.modalEdit = true
        },
        onSelectedKeyChange(selectedKey: String) {
          this.selectedKey = selectedKey
        }
      }
    });
</script>

