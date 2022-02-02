<template>
  <div>
    <Options
      v-if="updatedData"
      :eventBus="eventBus"
      :options="options"
      :editMode="editMode"
    />
    <JsonEmbed
      :output-data="updatedData"
      field-name="workflowJsonData"
    />
  </div>
</template>

<script lang="ts">
  import Vue from 'vue';
  import axios from "axios";
  import Options from '../../../components/job/workflow/Options.vue';
  import JsonEmbed from './JsonEmbed.vue';
  import {OptionData, ScheduledExecution, WorkflowData} from "@/components/job/workflow/Workflow";

  const w = window as any;

  export default Vue.extend({
    name: 'WorkflowEditorSection',
    props: {
      eventBus: Object,
      editMode: Boolean
    },
    components: {
      Options,
      JsonEmbed
    },
    computed: {
    },
    watch:{
      updatedData(){
        w.jobWasEdited()
      }
    },
    methods: {
      async fetchWorkflowData(): Promise<WorkflowData> {
        if (w._rundeck && w._rundeck.rdBase && w._rundeck.projectName) {
          this.rdBase = w._rundeck.rdBase;
          this.project = w._rundeck.projectName;
          if (w._rundeck.data) {
            this.workflowData = w._rundeck.data.workflowData
            return Object.assign({}, this.workflowData);
          }
        }
        return {} as WorkflowData;
      },
      createOptions(): OptionData[] {
        let options = [] as OptionData[]
       // if (options === null || options[0] === null || options[0].name === null) {
          options = this.scheduledExecutionData.options as OptionData[];
        //}
        return options;
      },
      fetchSchedEx(): object {
        let payload = {
          options: [] as OptionData[]
        };
        axios({
          method: "get",
          headers: {
            "Accept": "application/json",
            "Content-type": "application/json"
          },
          url: `${this.rdBase}/api/36/job/show/${this.schedExUUID}`,
          withCredentials: true
        }).then(response => {
          payload = JSON.parse(response.data)
        }).catch((error: string) => {
          this.errors.push(error)
        })
        return payload;
      }
    },
    async mounted() {
      this.updatedData = await this.fetchWorkflowData();
      this.schedExUUID = w._rundeck.rundeckClient.uri.split('/')[-1];
      this.scheduledExecutionData = this.fetchSchedEx();
      this.options = this.createOptions();
    },
    data () {
      return {
        errors: [] as any,
        project: "",
        rdBase: "",
        schedExUUID: "",
        scheduledExecutionData: <ScheduledExecution>{},
        options: [<OptionData>{}],
        workflowData: <WorkflowData>{},
        updatedData: <WorkflowData>{}
      };
    }
  });
</script>
