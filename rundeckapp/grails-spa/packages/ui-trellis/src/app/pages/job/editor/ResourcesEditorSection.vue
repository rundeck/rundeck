<template>
  <div >
    <ui-socket section="resources-editor" location="top" :event-bus="eventBus" :root-store="rootStore"/>
    <resources-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData"/>
    <json-embed :output-data="updatedData" field-name="resourcesJsonData"/>
  </div>
</template>

<script>
import { defineComponent } from 'vue'
import * as _ from 'lodash'

import ResourcesEditor from '../../../components/job/resources/ResourcesEditor.vue'
import UiSocket from '../../../../library/components/utils/UiSocket.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
} from "../../../../library"

export default defineComponent({
  name: 'ResourcesEditorSection',
  props:['eventBus'],
  components: {
    ResourcesEditor,
    JsonEmbed,
    UiSocket
  },
  data () {
    return {
      resourcesData: {},
      updatedData:null,
      watching:false
    }
  },
  computed: {
    rootStore() {
      return getRundeckContext().rootStore
    }
  },
  watch:{
    updatedData: {
      handler() {
        if (this.watching) {
          if (!_.isEqual(this.resourcesData, this.updatedData)) {
            window.jobWasEdited()
          }
        }
      },
      deep: true,
    }
  },
  async mounted () {
    if(window._rundeck && window._rundeck.data){
        this.resourcesData = window._rundeck.data.resourcesData
        this.updatedData = Object.assign({}, this.resourcesData)
        this.watching = true
    }
  }
})
</script>

<style>
</style>
