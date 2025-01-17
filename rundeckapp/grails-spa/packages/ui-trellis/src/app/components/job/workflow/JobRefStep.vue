<template>
  <div>
    <i class="glyphicon glyphicon-book"></i>
    {{ fullName }}
    <template v-if="step.jobref.project">
      ({{ step.jobref.project }})
    </template>
    <div v-if="step.jobref.args" class="argString">
      <template v-if="parsed">
        <template v-for="entry in parsed" :key="entry.key">
          <span class="optkey"> {{ entry.key }} </span>
          <code v-if="entry.value" class="optvalue">{{ entry.value }}</code>
        </template>
      </template>
      <code class="optvalue">{{ step.jobref.args }}</code>
    </div>
    <template v-if="step.nodeStep">
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

type OptionsMap = Record<string, string>;

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
    parsed() {
      if (!this.step.jobref.args) {
        return null;
      }
      return this.parseOptsFromArray(this.burst(this.step.jobref.args));
    },
  },
  methods: {
    burst(argstring: string) {
      return (
        argstring
          .match(/[^\s"']+|"([^"]*)"|'([^']*)'/g)
          ?.map((part) => part.replace(/^['"]|['"]$/g, "")) ?? []
      );
    },
    parseOptsFromArray(tokens: string[]): OptionsMap {
      return tokens.reduce(
        (acc: OptionsMap, token: string, index: number, arr: string[]) => {
          if (
            token.startsWith("-") &&
            token.length > 1 &&
            index + 1 < arr.length
          ) {
            const key = token.substring(1);
            acc[key] = arr[index + 1];
          }
          return acc;
        },
        {},
      );
    },
  },
});
</script>
<style scoped lang="scss"></style>
