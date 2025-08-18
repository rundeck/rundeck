<template>
  <log-filters
    id="globalLogFilters"
    v-model="model.LogFilter"
    title="Global Log Filters"
    subtitle="All workflow steps"
    :show-if-empty="true"
  />
</template>
<script lang="ts">
import { GlobalLogFiltersData } from "@/app/components/job/workflow/types/workflowTypes";
import { cloneDeep } from "lodash";
import { defineComponent } from "vue";
import LogFilters from "@/app/components/job/workflow/LogFilters.vue";
export default defineComponent({
  name: "WorkflowGlobalLogFilters",
  components: {
    LogFilters,
  },
  props: {
    modelValue: {
      type: Object,
      required: true,
      default: () => ({ LogFilter: [] }) as GlobalLogFiltersData,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      model: { LogFilter: [] } as GlobalLogFiltersData,
    };
  },
  watch: {
    model: {
      handler() {
        this.$emit("update:modelValue", this.model);
      },
      deep: true,
    },
  },
  async mounted() {
    this.model = cloneDeep(this.modelValue);
  },
});
</script>
