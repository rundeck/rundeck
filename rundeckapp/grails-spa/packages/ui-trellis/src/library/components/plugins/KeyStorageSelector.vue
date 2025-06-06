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
  <btn @click="openSelector()" data-testid="open-selector-btn">
    <slot>Select… <i class="glyphicon glyphicon-folder-open"></i></slot>
  </btn>
  <modal
    id="storage-file"
    v-model="modalOpen"
    :before-close="beforeModalClose"
    title="Select a Storage File"
    disabled="readOnly"
    auto-focus
    append-to-body
    cancel-text="Cancel"
    ok-text="Save"
  >
    <key-storage-view
      ref="keyStorageViewRef"
      :read-only="readOnly"
      :root-path="rootPath"
      :allow-upload="allowUpload"
      :model-value="modelValue"
      :storage-filter="storageFilter"
      @update:model-value="onSelectedKeyChange"
      @open-editor="openEditor"
    ></key-storage-view>
  </modal>
  <modal
    id="storageuploadkey"
    ref="modalEdit"
    v-model="modalEdit"
    title="Add or Upload a Key"
    :footer="false"
    auto-focus
    append-to-body
  >
    <key-storage-edit
      :upload-setting="uploadSetting"
      :root-path="rootPath"
      @cancel-editing="handleCancelEditing"
      @finish-editing="handleFinishEditing"
    ></key-storage-edit>
  </modal>
</template>

<script lang="ts">
import { defineComponent } from "vue";

import KeyStorageView from "../../components/storage/KeyStorageView.vue";
import KeyStorageEdit from "../../components/storage/KeyStorageEdit.vue";
import { UploadSetting } from "../storage/KeyStorageEdit.vue";

export default defineComponent({
  name: "KeyStorageSelector",
  components: { KeyStorageEdit, KeyStorageView },
  props: ["modelValue", "storageFilter", "allowUpload", "readOnly"],
  emits: ["update:modelValue"],
  data() {
    return {
      modalOpen: false,
      modalEdit: false,
      selectedKey: "" as string,
      rootPath: "keys",
      inputPath: "",
      invalid: false,
      errorMsg: "",
      uploadSetting: {} as UploadSetting,
    };
  },
  methods: {
    cleanPath(path: any) {
      if (path != null) {
        while (path.indexOf("/") == 0) {
          path = path.substring(1);
        }
      } else {
        return "";
      }
      return path;
    },
    beforeModalClose(args: string | Array<any>) {
      if (args === "ok") {
        // Save the selected value - Emit a key changed event with the selected value
        if (this.selectedKey !== this.modelValue) {
          this.$emit("update:modelValue", this.selectedKey);
        }
      }
      return true;
    },
    clean() {
      this.selectedKey = this.modelValue;
      this.inputPath = "";
      this.invalid = false;
      this.errorMsg = "";
    },
    handleFinishEditing(selectedKey: any) {
      // @ts-ignore
      this.$refs.keyStorageViewRef.loadKeys(selectedKey);
      this.modalEdit = false;
    },
    handleCancelEditing() {
      this.modalEdit = false;
    },
    closeSelector() {
      this.modalOpen = false;
    },
    openSelector() {
      this.modalOpen = true;
    },
    openEditor(uploadSetting: UploadSetting) {
      this.uploadSetting = uploadSetting;
      this.modalEdit = true;
    },
    onSelectedKeyChange(selectedKey: string) {
      this.selectedKey = selectedKey;
    },
  },
});
</script>
