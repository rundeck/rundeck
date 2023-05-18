<template>
  <edit-project-node-source-file
    class="form-horizontal"
    :index="index"
    :provider="nodeSource.type"
    :source-desc="nodeSource.resources.description"
    :file-format="modelFormat"
    :value="nodesText"
    :event-bus="eventBus"
    :error-message="errorMessage"
    @cancel="handleCancel"
    @save="handleSave"
    :saving="saving"
    v-if="nodeSource">
  </edit-project-node-source-file>
</template>
<script lang="ts">
import {getRundeckContext} from '../../../library'
import {NodeSource} from '../../../library/stores/NodeSourceFile'
import EditProjectNodeSourceFile from './EditProjectNodeSourceFile.vue'

export default Vue.extend({
  inject: ['nodeSourceFile'],
  components: {EditProjectNodeSourceFile},
  props: {
    index: {
      type: Number,
      required: true,
    },
    nextPageUrl: {
      type: String,
      required: true,
    },
    eventBus: {
      type: Object,
      required: true
    }
  },
  data() {
    return {
      nodeSource: null as NodeSource,
      mimeFormats: {
        'text/xml': 'xml',
        'application/xml': 'xml',
        'application/yaml': 'yaml',
        'text/yaml': 'yaml',
        'application/json': 'json',
      },
      nodesText: '',
      errorMessage: '',
      inited: false,
      saving: false
    }
  },
  computed: {
    modelFormat() {
      return this.nodeSourceFile.modelFormat
    }
  },
  methods: {
    handleCancel() {
      this.eventBus.$emit('page-reset', 'nodes')
      if (this.nextPageUrl) {
        window.location = this.nextPageUrl
      }
    },
    async handleSave(newVal) {
      if (this.saving) {
        return
      }
      this.errorMessage = ''
      this.nodesText = newVal
      this.eventBus.$emit('page-reset', 'nodes')
      let resp = await this.nodeSourceFile.storeSourceContent(this.nodesText)
      this.$notify('Content Saved')
      if (this.nextPageUrl) {
        window.location = this.nextPageUrl
      }
    },
    acceptContent(newVal) {
      console.log("acceptContent",newVal)
      if (newVal && typeof (newVal.nodesYaml) !== 'undefined' && this.nodesText !== newVal.nodesYaml) {
        this.nodesText = newVal.nodesYaml
        this.eventBus.$emit('node-source-file-content-loaded', this.nodesText)
        if (this.inited) {
          this.eventBus.$emit('page-modified', 'nodes')
        }
      }
    }
  },
  async mounted() {
    const context = getRundeckContext()
    if(this.index>=0) {
      this.nodeSourceFile.index = this.index
      await this.nodeSourceFile.load()
      this.nodeSource = this.nodeSourceFile.nodeSource
    }
    this.eventBus.$on('node-source-file-set-content', this.acceptContent)
    if (this.nodeSource && this.nodeSource.resources.writeable && this.modelFormat) {
      //load value

      await this.nodeSourceFile.retrieveSourceContent()
      this.acceptContent({nodesYaml: this.nodeSourceFile.content})
      this.eventBus.$emit('node-source-file-content-inited', this.nodesText)
      this.inited = true
    }
  }
})
</script>