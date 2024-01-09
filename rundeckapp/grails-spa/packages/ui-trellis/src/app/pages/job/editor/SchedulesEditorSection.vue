<template>
  <div >
    <schedule-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData" :use-crontab-string="useCrontabString"/>
    <json-embed :output-data="updatedData" field-name="schedulesJsonData"/>
  </div>
</template>

<script>
import ScheduleEditor from '../../../components/job/schedule/ScheduleEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
} from "../../../../library"

export default {
  name: 'App',
  props:['eventBus','useCrontabString' ],
  components: {
    ScheduleEditor,
    JsonEmbed
  },
  data () {
    return {
      project: null,
      rdBase: null,
      schedulesData: {},
      updatedData:null
    }
  },
  watch:{
    updatedData(){
      window.jobWasEdited()
    }
  },
  async mounted () {
    const rundeck = getRundeckContext()
    if (rundeck && rundeck.rdBase && rundeck.projectName) {
      this.rdBase = rundeck.rdBase
      this.project = rundeck.projectName
      if(rundeck && rundeck.data){
        this.schedulesData = rundeck.data.schedulesData
        this.updatedData = Object.assign({}, this.schedulesData)
      }
    }
  }
}
</script>

<style>
</style>
