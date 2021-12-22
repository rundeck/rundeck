<template>
  <div>
    <Options
      v-if="updatedData"
      v-model="updatedData"
      :event-bus="eventBus"
      :options=options
      :editMode=editMode
    />
    <JsonEmbed
      :output-data="updatedData"
      field-name="workflowJsonData"
    />
  </div>
</template>

<script lang="ts">
  import Vue from 'vue';
  import Options from '../../../components/job/workflow/Options.vue'
  import {getSchedEx} from '../../../services/scheduledExecution'
  import JsonEmbed from './JsonEmbed.vue'

  const w = window as any;

  export default Vue.extend({
    name: 'WorkflowEditorSection',
    props: {
      eventBus: Object,
      options: Object,
      editMode: Boolean
    },
    components: {
      Options,
      JsonEmbed
    },
    computed: {
      options() {
        let options = this.updatedData.sessionOpts;
        if (!options) {
          this.fetchWorkflowData();
          options = this.updatedData.scheduledExecution.options;
        };
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
          this.rdBase = w._rundeck.rdBase;
          this.project = w._rundeck.projectName;
          await getSchedEx(this.jobId)
            .then(data => {
              this.updatedData = data;
            }).catch((error) => {
              console.log(`Failed to get schedule data via API with error ${JSON.stringify(error)}/ngetting from embedded data...`);
              this.workflowData = w._rundeck.data.workflowData;
              this.updatedData = Object.assign({}, this.workflowData);
            });
        };
      }
    },
    async mounted() {
      this.fetchWorkflowData();
    },
    data () {
      return {
        project: null as any,
        rdBase: null as any,
        workflowData: {},
        updatedData: null as any,
        jobId: w._rundeck.rundeckClient.uri.split('/')[-1] as string,
      };
    }
  });
</script>
