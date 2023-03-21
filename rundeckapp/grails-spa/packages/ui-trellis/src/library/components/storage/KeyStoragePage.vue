<template>
<div>
  <key-storage-view ref="keyStorageViewRef" :project="project" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @openEditor="openEditor"></key-storage-view>
  <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body :footer="false">
    <key-storage-edit :project="this.project" :uploadSetting="uploadSetting" :storage-filter="storageFilter" @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing"></key-storage-edit>
  </modal>
</div>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit from "./KeyStorageEdit.vue";
import Vue from "vue";

export default Vue.extend({
  name: "KeyStoragePage",
  components: { KeyStorageEdit, KeyStorageView },
  props: [
    'readOnly',
    'allowUpload',
    'value',
    'storageFilter'
  ],
  data() {
    return {
      modalEdit: false,
      path: '',
      uploadSetting: {},
      project: ''
    }
  },
  methods: {
    handleFinishEditing(){
      // @ts-ignore
      this.$refs.keyStorageViewRef.loadKeys();
      this.modalEdit = false
    },
    handleCancelEditing() {
      this.modalEdit = false
    },
    openEditor(uploadSetting: {}) {
      this.uploadSetting = uploadSetting
      this.modalEdit = true
    },
  },
  async mounted() {
    this.project = window._rundeck.projectName;
    this.path=this.value ? this.value : ""
  }
})
</script>
