<template>
  <div>
    <i class="glyphicon glyphicon-book"></i>
    {{ fullName }}
    <template v-if="step.jobref.project">
      ({{ step.jobref.project }})
    </template>
    <div class="argString" v-if="step.jobref.args">
      <!--      <g:render template="/execution/execArgString" model="[argString: item.argString]"/>-->
      <!--      TODO: job option parse-->
      <code class="optvalue">{{ step.jobref.args }}</code>
    </div>
    <template v-if="step.jobref.nodeStep">
      <i class="fas fa-hdd"></i>
      <span class="info note">
        {{ $t("JobExec.nodeStep.true.label") }}
      </span>
    </template>
  </div>
</template>
<script lang="ts">
import { JobRefData } from "@/app/components/job/workflow/types/workflowTypes";
import { defineComponent, PropType } from "vue";

export default defineComponent({
  name: "JobRefStep",
  props: {
    step: {
      type: Object as PropType<JobRefData>,
      required: true,
      default: () => ({}) as JobRefData,
    },
  },
  computed: {
    fullName() {
      return (
        (this.step.jobref.group ? this.step.jobref.group + "/" : "") +
        this.step.jobref.name
      );
    },
  },
});
</script>
<style scoped lang="scss"></style>
