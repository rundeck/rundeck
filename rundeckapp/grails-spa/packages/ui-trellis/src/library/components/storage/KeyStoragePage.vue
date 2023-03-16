<template>
  <div v-if="viewMode">
    <key-storage-view :project="project" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @closeEditor="closeEditor" @openEditor="openEditor"></key-storage-view>
  </div>
  <div v-if="editMode">
    <key-storage-edit :storage-filter="storageFilter" @closeEditor="closeEditor"></key-storage-edit>
  </div>

</template>

<script>
import KeyStorageView from "../../../../build/pack/src/library/components/storage/KeyStorageView";
import KeyStorageEdit from "../../../../build/pack/src/library/components/storage/KeyStorageEdit";
export default {
  name: "KeyStoragePage",
  components: {KeyStorageEdit, KeyStorageView},
  props: [
    'readOnly',
    'allowUpload',
    'value',
    'storageFilter',
    'project'
  ],
  data() {
    return {
      viewMode: true,
      editMode: false,
      path: ''
    }
  },
  methods: {
    closeEditor(){
      this.editMode=false
      this.viewMode=true
    },
    openEditor(){
      this.viewMode=false
      this.editMode=true
    }
  },
  async mounted() {
    this.path=this.value ? this.value : ""
  }
}
</script>

<style scoped>

</style>