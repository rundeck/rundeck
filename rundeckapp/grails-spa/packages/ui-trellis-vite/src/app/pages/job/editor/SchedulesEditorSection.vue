<template>
  <div >
    <schedule-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData" :use-crontab-string="useCrontabString"/>
    <json-embed :output-data="updatedData" field-name="schedulesJsonData"/>
  </div>
</template>

<script>
import ScheduleEditor from '../../../components/job/schedule/ScheduleEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

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
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      this.project = window._rundeck.projectName
      if(window._rundeck && window._rundeck.data){
        this.schedulesData = window._rundeck.data.schedulesData
        this.updatedData = Object.assign({}, this.schedulesData)
      }
    }
  }
}
</script>

<style>
</style>
