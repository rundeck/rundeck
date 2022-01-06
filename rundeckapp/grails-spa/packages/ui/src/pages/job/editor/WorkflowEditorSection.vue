<template>
  <div>
    <Options
      v-if="updatedData"
      :eventBus="eventBus"
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
  import VueCompositionAPI from '@vue/composition-api';
  import { defineComponent, ref, reactive } from '@vue/composition-api';
  import Options from '../../../components/job/workflow/Options.vue';
  import JsonEmbed from './JsonEmbed.vue';
  import {
    getRundeckContext,
    RundeckContext
  } from "@rundeck/ui-trellis";
  import {OptionData, WorkflowData} from "@/components/job/workflow/Workflow";

  const w = window as any;
  const winRd = getRundeckContext() as any;

  export default Vue.extend({
    name: 'WorkflowEditorSection',
    props: {
      eventBus: Object,
      recievedOptions: Array,
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
      async fetchWorkflowData(): Promise<void> {
        if (winRd && winRd.rdBase && winRd.projectName) {
          this.rdBase = winRd.rdBase;
          this.project = winRd.projectName;
          this.updatedData = Object.assign(this, winRd.data.workflowData);
        };
      },
      async fetchOptions(): Promise<any> {
        let options = this.updatedData.sessionOpts as any;
        if (!options) {
          await this.fetchWorkflowData();
          options = this.updatedData.scheduledExecution.options as any;
        }
        return options;
      }
    },
    async mounted() {
      await this.fetchOptions();
      await this.fetchWorkflowData();
    },
    data () {
      return {
        project: "",
        rdBase: "",
        options: [<OptionData>{}],
        updatedData: <WorkflowData>{}
      };
    }
  });
</script>
