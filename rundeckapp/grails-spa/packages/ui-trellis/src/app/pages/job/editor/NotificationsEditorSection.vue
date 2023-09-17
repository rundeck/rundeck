<template>
  <div >
    <notifications-editor :notification-data="notificationData" v-if="notificationData" @changed="changed"/>
    <json-embed :output-data="updatedData.notifications" field-name="jobNotificationsJson"/>
  </div>
</template>

<script>
import * as _ from 'lodash'
import NotificationsEditor from '../../../components/job/notifications/NotificationsEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
} from "../../../../library"

export default {
  name: 'App',
  props:['eventBus' ],
  components: {
    NotificationsEditor,
    JsonEmbed
  },
  data () {
    return {
      notificationData: null,
      updatedData: {
          notifications:[],
          notifyAvgDurationThreshold:null
      }
    }
  },
  methods:{
    changed(data){
        if (!_.isEqual(data, this.updatedData.notifications)) {
            this.updatedData.notifications = data
            //nb: hook to indicate job was editted, defined in jobedit.js
            window.jobWasEdited()
        }
    }
  },
  async mounted () {
     if(getRundeckContext() && getRundeckContext().data){
        this.notificationData = getRundeckContext().data.notificationData
        this.updatedData=this.notificationData
      }
  }
}
</script>

<style>
</style>
