<template>
  <div >
    <other-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData"/>
    <json-embed :output-data="updatedData" field-name="otherJsonData"/>
  </div>
</template>

<script>
import { defineComponent } from 'vue'
import * as _ from 'lodash'

import OtherEditor from '../../../components/job/other/OtherEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

import {
  getRundeckContext,
} from "../../../../library"

export default defineComponent({
  name: 'App',
  props:['eventBus' ],
  components: {
    OtherEditor,
    JsonEmbed
  },
  data () {
    return {
      otherData: {},
      updatedData:null,
      watching:false
    }
  },
  watch:{
    updatedData: {
      handler() {
        if (this.watching) {
          if (!_.isEqual(this.otherData, this.updatedData)) {
            window.jobWasEdited()
          }
        }
      },
      deep: true,
    }
  },
  mounted () {
    if(getRundeckContext() && getRundeckContext().data){
        this.otherData = getRundeckContext().data.otherData
        this.updatedData = Object.assign({}, this.otherData)
        this.watching = true
    }
  }
})
</script>

<style>
</style>
