<template>
  <div >
    <options-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData.active"/>
    <json-embed :output-data="updatedData" field-name="optionsJsonData"/>
  </div>
</template>

<script>
import OptionsEditor from '../../../components/job/options/OptionsEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"
import Vue from "vue";

export default {
  name: 'App',
  props:['eventBus' ],
  components: {
    OptionsEditor,
    JsonEmbed
  },
  data () {
    return {
      optionsData: {},
      updatedData: {},
      watching:false,
      active: false,
    }
  },
  watch:{
    updatedData(){
        if(this.watching) {
            if(!_.isEqual(this.optionsData,this.updatedData)){
                window.jobWasEdited()
            }
        }
    }
  },
  async mounted () {
    if(window._rundeck && window._rundeck.data){
        this.optionsData = window._rundeck.data.optionsData
        this.updatedData = Object.assign({}, this.optionsData)
        Vue.set(this.updatedData, 'active', true)

      this.watching = true
    }

    this.eventBus.$on('job-section-edit',()=>{
      Vue.set(this.updatedData, 'active', true)
    })
  }

}
</script>

<style>
</style>
