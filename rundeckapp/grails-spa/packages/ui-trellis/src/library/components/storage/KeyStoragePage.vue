<template>
  <ui-socket section="edit-key-storage-providers" location="viewKeys">
  <div>
    <key-storage-view ref="keyStorageViewRef" v-if="ready" :project="project" :createdKey="selectedKey" :root-path="rootPath" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @openEditor="openEditor"></key-storage-view>
    <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body :footer="false">
      <key-storage-edit :project="project" :root-path="rootPath" :uploadSetting="uploadSetting" :storage-filter="storageFilter" @keyCreated="updateSelectedKey"  @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing"></key-storage-edit>
    </modal>
  </div>
  </ui-socket>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit, { UploadSetting } from "./KeyStorageEdit.vue";
import {defineComponent} from "vue";
import { getRundeckContext } from "../../index"
import UiSocket from "../utils/UiSocket.vue";

export default defineComponent({
  name: "KeyStoragePage",
  components: {UiSocket, KeyStorageEdit, KeyStorageView},
  props: [
    'readOnly',
    'allowUpload',
    'value',
    'storageFilter',
    'project'
  ],
  data() {
    return {
      activeTab: 'keys',
      tabs: [
        { id: 'keys', name: 'Keys' },
        { id: 'configure', name: 'Configure' },
      ],
      modeToggle: {
        type: Boolean,
        default: true
      },
      configPrefix: '',
      bus: getRundeckContext().eventBus,
      modalEdit: false,
      path: '',
      uploadSetting: {} as UploadSetting,
      ready: false,
      selectedKey: {}
    }
  },
  methods: {
    handleFinishEditing(selectedKey: any) {
      // @ts-ignore
      this.$refs.keyStorageViewRef.loadKeys(selectedKey);
      this.modalEdit = false
    },
    handleCancelEditing() {
      this.modalEdit = false
    },
    openEditor(uploadSetting: UploadSetting) {
      this.uploadSetting = uploadSetting
      this.modalEdit = true
    },
    updateSelectedKey(key: {}) {
      this.selectedKey = key
    },
  },
  computed: {
    rootPath(): string {
      return this.project ? "keys/project/" + this.project : "keys"
    }
  },
  async mounted() {
    this.path = this.value ? this.value : ""
    this.ready = true
  }
})
</script>
