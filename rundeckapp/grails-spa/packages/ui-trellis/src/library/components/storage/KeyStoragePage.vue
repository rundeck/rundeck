<template>
  <div>
    <div class="vue-tabs">
      <div class="nav-tabs-navigation">
        <div class="nav-tabs-wrapper">
          <ul class="nav nav-tabs" id="job_edit_tabs">
            <li v-for="tab in tabs" :key="tab.id" :class="{ 'active': activeTab === tab.id }">
              <a href="#" @click.prevent="activeTab = tab.id">
                {{ tab.name }}
              </a>
            </li>
          </ul>
        </div>
      </div>

      <!-- Key Tab -->
      <div v-show="activeTab === 'keys'">
        <key-storage-view ref="keyStorageViewRef" v-if="ready" :project="project" :created-key="selectedKey" :root-path="rootPath" :read-only="readOnly" :allow-upload="allowUpload" :value="path" @openEditor="openEditor"></key-storage-view>
        <modal v-model="modalEdit" title="Add or Upload a Key" id="storageuploadkey" ref="modalEdit" auto-focus append-to-body :footer="false">
          <key-storage-edit :project="this.project" :root-path="rootPath" :upload-setting="uploadSetting" :storage-filter="storageFilter" @keyCreated="updateSelectedKey"  @cancelEditing="handleCancelEditing" @finishEditing="handleFinishEditing"></key-storage-edit>
        </modal>
      </div>

      <!-- Configure Tab -->
      <div v-show="activeTab === 'configure'">
        <key-storage-plguin-configuration
            :configPrefix="configPrefix"
            service-name="Storage"
            :edit-mode="true"
            :help="$t('help')"
            project=""
            :edit-button-text="$t('Edit Plugin Groups')"
            :mode-toggle="false"
        >
        </key-storage-plguin-configuration>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import KeyStorageView from "./KeyStorageView.vue";
import KeyStorageEdit from "./KeyStorageEdit.vue";
import KeyStoragePlguinConfiguration from "./KeyStoragePlguinConfiguration.vue";
import Vue from "vue";

export default Vue.extend({
  name: "KeyStoragePage",
  components: {KeyStorageEdit, KeyStorageView, KeyStoragePlguinConfiguration},
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
