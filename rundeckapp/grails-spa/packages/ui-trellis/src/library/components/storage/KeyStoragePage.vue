<template>
  <ui-socket section="edit-key-storage-providers" location="viewKeys">
  <div>
    <key-storage-view ref="keyStorageViewRef" v-if="ready" :project="project" :createdKey="selectedKey" :root-path="rootPath" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @openEditor="openEditor"></key-storage-view>
    <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body :footer="false">
      <key-storage-edit :project="this.project" :root-path="rootPath" :uploadSetting="uploadSetting" :storage-filter="storageFilter" @keyCreated="updateSelectedKey"  @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing"></key-storage-edit>
    </modal>
  </div>
  </ui-socket>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit from "./KeyStorageEdit.vue";
import Vue from "vue";
import UiSocket from "../utils/UiSocket.vue";

export default Vue.extend({
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
      bus: new Vue(),
      modalEdit: false,
      path: '',
      uploadSetting: {},
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
    openEditor(uploadSetting: {}) {
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
