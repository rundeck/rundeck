<template>
  <ui-socket section="edit-key-storage-providers" location="viewKeys">
    <div>
      <key-storage-view
        v-if="ready"
        ref="keyStorageViewRef"
        :project="project"
        :created-key="selectedKey"
        :root-path="rootPath"
        :read-only="readOnly"
        :allow-upload="allowUpload"
        :value="path"
        @open-editor="openEditor"
      ></key-storage-view>
      <modal
        id="storageuploadkey"
        ref="modalEdit"
        v-model="modalEdit"
        title="Add or Upload a Key"
        auto-focus
        append-to-body
        :footer="false"
      >
        <key-storage-edit
          :project="project"
          :root-path="rootPath"
          :upload-setting="uploadSetting"
          :storage-filter="storageFilter"
          @key-created="updateSelectedKey"
          @cancel-editing="handleCancelEditing"
          @finish-editing="handleFinishEditing"
        ></key-storage-edit>
      </modal>
    </div>
  </ui-socket>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit, { UploadSetting } from "./KeyStorageEdit.vue";
import { defineComponent } from "vue";
import { getRundeckContext } from "../../index";
import UiSocket from "../utils/UiSocket.vue";

export default defineComponent({
  name: "KeyStoragePage",
  components: { UiSocket, KeyStorageEdit, KeyStorageView },
  props: ["readOnly", "allowUpload", "modelValue", "storageFilter", "project"],
  data() {
    return {
      activeTab: "keys",
      tabs: [
        { id: "keys", name: "Keys" },
        { id: "configure", name: "Configure" },
      ],
      modeToggle: {
        type: Boolean,
        default: true,
      },
      configPrefix: "",
      bus: getRundeckContext().eventBus,
      modalEdit: false,
      path: "",
      uploadSetting: {} as UploadSetting,
      ready: false,
      selectedKey: {},
    };
  },
  computed: {
    rootPath(): string {
      return this.project ? "keys/project/" + this.project : "keys";
    },
  },
  async mounted() {
    this.path = this.value ? this.value : "";
    this.ready = true;
  },
  methods: {
    handleFinishEditing(selectedKey: any) {
      // @ts-ignore
      this.$refs.keyStorageViewRef.loadKeys(selectedKey);
      this.modalEdit = false;
    },
    handleCancelEditing() {
      this.modalEdit = false;
    },
    openEditor(uploadSetting: UploadSetting) {
      this.uploadSetting = uploadSetting;
      this.modalEdit = true;
    },
    updateSelectedKey(key: {}) {
      this.selectedKey = key;
    },
  },
});
</script>
