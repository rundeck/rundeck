<template>
    <edit-project-node-source-file
            class="form-horizontal"
            :index="index"
            :provider="nodeSource.type"
            :source-desc="nodeSource.resources.description"
            :file-format="modelFormat"
            :value="nodesText"
            @cancel="handleCancel"
            @save="handleSave"
            v-if="nodeSource">
    </edit-project-node-source-file>
</template>
<script lang="ts">
import {getRundeckContext} from '../../../library'
import EditProjectNodeSourceFile from './EditProjectNodeSourceFile.vue'
import {
  getProjectNodeSource,
  getProjectWriteableNodeSourceText,
  NodeSource, saveProjectWriteableNodeSourceText
} from '../project-nodes-config/nodeSourcesUtil'

export default Vue.extend({
  components: {EditProjectNodeSourceFile},
  props: {},
  data() {
    return {
      index:-1,
      nodeSource: null as NodeSource,
      mimeFormats: {
        'text/xml': 'xml',
        'application/xml': 'xml',
        'application/yaml': 'yaml',
        'text/yaml': 'yaml',
        'application/json': 'json',
      },
      nodesText: '',
      nextPageUrl:''
    }
  },
  computed: {
    modelFormat() {
      if(this.nodeSource) {
        return this.mimeFormats[this.nodeSource.resources.syntaxMimeType] || this.nodeSource.resources.syntaxMimeType || ''
      }
      return ''
    }
  },
  methods: {
    handleCancel() {
      console.log('cancel')
    },
    async handleSave(newVal) {
      this.nodesText=newVal
      let resp = await saveProjectWriteableNodeSourceText(this.index, this.nodeSource.resources.syntaxMimeType, this.nodesText)
      this.$notify('Content Saved')
      if(this.nextPageUrl){
        window.location=this.nextPageUrl
      }
    }
  },
  async mounted() {
    const context = getRundeckContext()
    if (context.data && context.data.editProjectNodeSourceData) {
      this.index = context.data.editProjectNodeSourceData.index
      this.nextPageUrl=context.data.editProjectNodeSourceData.nextPageUrl
    }else if(typeof(loadJsonData)==='function'){
      const data = loadJsonData('editProjectNodeSourceData')
      this.index=data.index
      this.nextPageUrl=data.nextPageUrl
    }
    if(this.index>=0) {
      this.nodeSource = await getProjectNodeSource(this.index)
    }
    if (this.nodeSource && this.nodeSource.resources.writeable && this.modelFormat) {
      //load value
      this.nodesText = await getProjectWriteableNodeSourceText(this.index, this.nodeSource.resources.syntaxMimeType)
    }
  }
})
</script>