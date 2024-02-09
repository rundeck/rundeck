<template>
  <dropdown v-if="filters">
    <btn class="dropdown-toggle" size="sm">
      Filters
      <i class="caret" />
    </btn>
    <template #dropdown>
      <template v-if="jobPageStore.selectedFilter">
        <li>
          <a role="button" @click="deleteSelectedFilter">
            <b class="glyphicon glyphicon-trash"></b>
            {{
              $t("job.list.filter.delete.filter.link.text", [
                jobPageStore.selectedFilter,
              ])
            }}
          </a>
        </li>
        <li role="separator" class="divider"></li>
      </template>
      <li class="dropdown-header">
        <i class="glyphicon glyphicon-filter"></i>
        {{ $t("saved.filters") }}
      </li>
      <li v-for="item in filters" :key="item.name">
        <a
          role="button"
          :class="{ active: item.active }"
          @click.prevent="selectFilter(item.name)"
        >
          <i
            v-if="jobPageStore.selectedFilter === item.name"
            class="glyphicon glyphicon-check"
          >
          </i>
          {{ item.name }}
        </a>
      </li>
    </template>
  </dropdown>
</template>

<script lang="ts">
import {
  JobListFilter,
  JobListFilterStore,
  JobListFilterStoreInjectionKey,
} from "@/library/stores/JobListFilterStore";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, inject, ref } from "vue";

export default defineComponent({
  name: "JobPageFiltersPopup",
  emits: ["select", "delete"],
  setup(props) {
    const jobListFilterStore: JobListFilterStore = inject(
      JobListFilterStoreInjectionKey,
    ) as JobListFilterStore;
    const jobPageStore: JobPageStore = inject(
      JobPageStoreInjectionKey,
    ) as JobPageStore;
    return {
      jobListFilterStore,
      jobPageStore,
      filters: ref<JobListFilter[]>([]),
    };
  },
  computed: {},
  async mounted() {
    await this.jobListFilterStore.load();
    this.filters = this.jobListFilterStore.getFilters();
  },
  methods: {
    selectFilter(name: string) {
      this.$emit("select", name);
    },
    deleteSelectedFilter() {
      this.$emit("delete", this.jobPageStore.selectedFilter);
      this.filters = this.jobListFilterStore.getFilters();
    },
  },
});
</script>

<style scoped lang="scss"></style>
