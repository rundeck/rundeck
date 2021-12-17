<template>
  <div >
    <details-options v-model="updatedData" :event-bus="eventBus" :options=options :editMode=editMode v-if="updatedData"/>
    <json-embed :output-data="updatedData" field-name="workflowJsonData"/>
  </div>
</template>

<script lang="ts">
import Vue from 'vue';
import DetailsOptions from '../../../components/job/workflow/DetailsOptions.vue'
import {getSchedEx} from '../../../services/scheduledExecution'
import JsonEmbed from './JsonEmbed.vue'

const w = window;

export default Vue.extend({
  name: 'WorkflowEditorSection',
  props: {
    eventBus: Object,
    options: Object,
    editMode: Boolean
  },
  components: {
    DetailsOptions,
    JsonEmbed
  },
  data () {
    return {
      project: null as any,
      rdBase: null as any,
      workflowData: {},
      updatedData: null as any,
      jobId: w._rundeck.rundeckClient.uri.split('/')[-1] as string,
    }
  },
  computed: {
    options() {
      let options = this.updatedData.sessionOpts;
      if (!options) {
        this.fetchWorkflowData()
        options = this.updatedData.scheduledExecution.options
      }
      return options;
    }
  },
  watch:{
    updatedData(){
      w.jobWasEdited()
    }
  },
  methods: {
    async fetchWorkflowData() {
      if (w._rundeck && w._rundeck.rdBase && w._rundeck.projectName) {
      this.rdBase = w._rundeck.rdBase
      this.project = w._rundeck.projectName
        await getSchedEx(this.jobId).then(data => {
          this.updatedData = data
        }).catch((error) => {
          console.log(`Failed to get schedule data via API, getting from embedded JSON... Error was ${JSON.stringify(error)}`);
          if(w._rundeck && w._rundeck.data){
            this.workflowData = w._rundeck.data.workflowData
            this.updatedData = Object.assign({}, this.workflowData)
          }
      })
    }
    }
  },
  async mounted() {
    this.fetchWorkflowData();
  }
})
</script>

<style>
</style>
