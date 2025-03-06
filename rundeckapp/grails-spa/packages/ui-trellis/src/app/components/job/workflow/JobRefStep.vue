<template>
  <div class="flex flex-col">
    <p>
      <i class="glyphicon glyphicon-book"></i>
      {{ fullName }}
      <span class="info" v-if="step.jobref.project">
        ({{ step.jobref.project }})
      </span>
    </p>
    <p v-if="step.jobref.args" class="argString">
      <template v-if="parsed">
        <template v-for="(entry, key) in parsed" :key="key">
          <span class="optkey"> {{ key }} </span>
          <code v-if="entry" class="optvalue">{{ entry }}</code>
        </template>
      </template>
      <code v-else class="optvalue">{{ step.jobref.args }}</code>
    </p>
    <p v-if="step.jobref.nodeStep">
      <i class="fas fa-hdd"></i>
      <span class="info note">
        {{ $t("JobExec.nodeStep.true.label") }}
      </span>
    </p>
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
      if (this.step.jobref.name) {
        return (
          (this.step.jobref.group ? this.step.jobref.group + "/" : "") +
          this.step.jobref.name
        );
      }
      return this.step.jobref.uuid;
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
<style scoped lang="scss">
p {
  margin-bottom: 0;
}
.info {
  margin-left: 5px;
}
</style>
