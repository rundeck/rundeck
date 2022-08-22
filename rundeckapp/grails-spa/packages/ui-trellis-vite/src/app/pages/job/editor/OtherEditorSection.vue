<template>
  <div >
    <other-editor v-model="updatedData" :event-bus="eventBus" v-if="updatedData"/>
    <json-embed :output-data="updatedData" field-name="otherJsonData"/>
  </div>
</template>

<script>
import OtherEditor from '../../../components/job/other/OtherEditor.vue'
import JsonEmbed from './JsonEmbed.vue'

export default {
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
    updatedData(){
        if(this.watching) {
            if(!_.isEqual(this.otherData,this.updatedData)){
                window.jobWasEdited()
            }
        }
    }
  },
  async mounted () {
    if(window._rundeck && window._rundeck.data){
        this.otherData = window._rundeck.data.otherData
        this.updatedData = Object.assign({}, this.otherData)
        this.watching = true
    }
  }
}
</script>

<style>
</style>
