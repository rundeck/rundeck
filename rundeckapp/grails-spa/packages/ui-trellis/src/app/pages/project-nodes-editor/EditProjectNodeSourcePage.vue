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
import {defineComponent, PropType} from 'vue'
import {EventBus, getRundeckContext} from '../../../library'
import {NodeSource} from '../../../library/stores/NodeSourceFile'
import EditProjectNodeSourceFile from './EditProjectNodeSourceFile.vue'

export default defineComponent({
  name: 'EditProjectNodeSourcePage',
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
      type: Object as PropType<typeof EventBus>,
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
      this.eventBus.emit('page-reset', 'nodes')
      window.location = this.nextPageUrl
    },
    async handleSave(newVal) {
      if (this.saving) {
        return
      }
      this.errorMessage = ''
      this.nodesText = newVal
      this.saving = true
      try {
        let resp = await this.nodeSourceFile.storeSourceContent(this.nodesText)
        this.eventBus.emit('page-reset', 'nodes')
        this.$notify('Content Saved')
        window.location = this.nextPageUrl
      } catch (e) {
        this.saving = false
        if (e.response && e.response.data && e.response.data) {
          //extract error message from API if present
          let errData = e.response.data
          if (typeof (errData) === 'string') {
            errData = JSON.parse(e.response.data)
          }
          if (errData.message) {
            this.errorMessage = errData.message
          } else if (errData.errorCode) {
            this.errorMessage = `An error occurred. Error code: ${errData.errorCode}`
          }
        } else if (e.response && e.response.status == 400) {
          this.errorMessage = `The content was an invalid format`
        } else {
          this.errorMessage = e.message
        }
      }
    },
    acceptContent(newVal) {
      if (newVal && typeof (newVal.content) === 'string' && this.nodesText !== newVal.content) {
        this.nodesText = newVal.content
        this.eventBus.emit('node-source-file-content-loaded', this.nodesText)
        if (this.inited) {
          this.eventBus.emit('page-modified', 'nodes')
        }
      }
    }
  },
  async mounted() {
    const context = getRundeckContext()
    if (this.index >= 0) {
      this.nodeSourceFile.index = this.index
      await this.nodeSourceFile.load()
      this.eventBus.emit('node-source-file-loaded', this.nodeSourceFile)
      this.nodeSource = this.nodeSourceFile.nodeSource
    }
    this.eventBus.on('node-source-file-set-content', this.acceptContent)
    if (this.nodeSource && this.nodeSource.resources.writeable && this.modelFormat) {
      await this.nodeSourceFile.retrieveSourceContent()
      this.acceptContent({content: this.nodeSourceFile.content})
      this.eventBus.emit('node-source-file-content-inited', this.nodesText)
      this.inited = true
    }
  }
})
</script>