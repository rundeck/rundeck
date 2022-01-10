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
  import Options from '../../../components/job/workflow/Options.vue';
  import JsonEmbed from './JsonEmbed.vue';
  import {OptionData, WorkflowData} from "@/components/job/workflow/Workflow";

  const w = window as any;

  export default Vue.extend({
    name: 'WorkflowEditorSection',
    props: {
      "event-bus": Object,
      "edit-mode": Boolean
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
        };
        return {} as WorkflowData;
      },
      async fetchOptions(): Promise<OptionData[]> {
        this.updatedData = await this.fetchWorkflowData();
        let options = this.updatedData.sessionOpts as OptionData[];
        if (options != null && !options[0].name) {
          options = this.updatedData.scheduledExecution.options as OptionData[];
        } else {
          options = [{}] as OptionData[]
        }
        return options;
      }
    },
    async mounted() {
      this.options = await this.fetchOptions();
    },
    data () {
      return {
        project: "",
        rdBase: "",
        options: [<OptionData>{}],
        workflowData: <WorkflowData>{},
        updatedData: <WorkflowData>{}
      };
    }
  });
</script>
