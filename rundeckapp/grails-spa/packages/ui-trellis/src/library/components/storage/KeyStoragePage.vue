<template>
  <div class="card">
    <div class="card-content vue-tabs">
      <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
          <ul class="nav nav-tabs" id="keyStorage_config_tabs">
            <li id="tab_storage_view">
              <div>
                <key-storage-view ref="keyStorageViewRef" v-if="ready" :project="project" :createdKey="selectedKey"
                                  :root-path="rootPath" :read-only="readOnly" :allow-upload="allowUpload" :value="path"
                                  @openEditor="openEditor"></key-storage-view>
                <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus
                       append-to-body :footer="false">
                  <key-storage-edit :project="this.project" :root-path="rootPath" :uploadSetting="uploadSetting"
                                    :storage-filter="storageFilter" @keyCreated="updateSelectedKey"
                                    @cancelEditing="handleCancelEditing"
                                    @finishEditing="handleFinishEditing"></key-storage-edit>
                </modal>
              </div>
            </li>
            <li id="tab_storage_plugin_configure">
              <div>
                <key-storage-plugin-page >

                </key-storage-plugin-page>
              </div>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit from "./KeyStorageEdit.vue";
import Vue from "vue";
import {getRundeckContext} from "../../index"
import KeyStoragePluginPage from "./KeyStoragePluginPage.vue";

export default Vue.extend({
  name: "KeyStoragePage",
  components: {KeyStoragePluginPage, KeyStorageEdit, KeyStorageView},
  props: [
    'readOnly',
    'allowUpload',
    'value',
    'storageFilter',
    'project'
  ],
  data() {
    return {
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
