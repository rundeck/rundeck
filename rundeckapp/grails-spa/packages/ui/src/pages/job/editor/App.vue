<template>
  <div >
    <notifications-editor :notification-data="notificationData" :event-bus="eventBus" v-if="notificationData" @changed="changed"/>
    <json-embed :output-data="updatedData" field-name="jobNotificationsJson"/>
  </div>
</template>

<script>
import NotificationsEditor from '../../../components/job/notifications/NotificationsEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

export default {
  name: 'App',
  props:['eventBus' ],
  components: {
    NotificationsEditor,
    JsonEmbed
  },
  data () {
    return {
      project: null,
      rdBase: null,
      notificationData: null,
      updatedData:null
    }
  },
  methods:{
    changed(data){
      this.updatedData=data
      //nb: hook to indicate job was editted, defined in jobedit.js
      window.jobWasEdited()
    }
  },
  async mounted () {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.projectName) {
      this.rdBase = window._rundeck.rdBase
      this.project = window._rundeck.projectName
      if(window._rundeck && window._rundeck.data){
        this.notificationData = window._rundeck.data.notificationData
        this.updatedData=this.notificationData
      }
    }
  }
}
</script>

<style>
</style>
