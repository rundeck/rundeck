<template>
<div>
  <key-storage-view :project="project" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @closeEditor="closeEditor" @openEditor="openEditor"></key-storage-view>
  <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body cancel-text="Cancel" ok-text="Save">
    <key-storage-edit :storage-filter="storageFilter" @closeEditor="closeEditor"></key-storage-edit>
  </modal>
</div>
</template>

<script>
import KeyStorageView from "./KeyStorageView";
import KeyStorageEdit from "./KeyStorageEdit";
export default {
  name: "KeyStoragePage",
  components: { KeyStorageEdit, KeyStorageView },
  props: [
    'readOnly',
    'allowUpload',
    'value',
    'storageFilter',
    'project'
  ],
  data() {
    return {
      modalEdit: false,
      path: ''
    }
  },
  methods: {
    closeEditor(){
      this.modalEdit=true
    },
    openEditor(){
      this.modalEdit=true
    }
  },
  async mounted() {
    this.path=this.value ? this.value : ""
  }
}
</script>
