<template>
  <div >
    <resources-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData"/>
    <json-embed :output-data="updatedData" field-name="resourcesJsonData"/>
  </div>
</template>

<script>
import ResourcesEditor from '../../../components/job/resources/ResourcesEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

export default {
  name: 'App',
  props:['eventBus' ],
  components: {
    ResourcesEditor,
    JsonEmbed
  },
  data () {
    return {
      project: null,
      rdBase: null,
      resourcesData: {},
      updatedData:null
    }
  },
  watch:{
    updatedData(){
      window.jobWasEdited()
    }
  },
  async mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      this.project = window._rundeck.projectName
      if(window._rundeck && window._rundeck.data){
        this.resourcesData = window._rundeck.data.resourcesData
        this.updatedData = Object.assign({}, this.resourcesData)
      }
    }
  }
}
</script>

<style>
</style>
