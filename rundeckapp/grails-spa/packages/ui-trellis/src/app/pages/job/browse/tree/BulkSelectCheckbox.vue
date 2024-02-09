<template>
  <span v-if="jobPageStore.bulkEditMode">
    <input v-model="selected" type="checkbox" class="checkbox-inline" />
  </span>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem, JobBrowseMeta } from "@/library/types/jobs/JobBrowse";
import { defineComponent, inject, ref } from "vue";

const eventBus = getRundeckContext().eventBus;
export default defineComponent({
  name: "BulkSelectCheckbox",
  props: {
    itemData: {
      type: Object,
      default: () => {},
    },
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
      selected: ref(false),
      subs: {},
    };
  },
  computed: {
    job(): JobBrowseItem {
      return this.itemData?.job;
    },
    authz(): Object | undefined {
      const data: any = this.job?.meta?.find(
        (meta: JobBrowseMeta) => meta.name === "authz",
      )?.data;
      return data || {};
    },
  },
  watch: {
    selected(val: boolean) {
      if (val) {
        this.jobPageStore.addBulkJob(this.job);
      } else {
        this.jobPageStore.removeBulkJob(this.job);
      }
    },
  },
  mounted() {
    this.subs["job-bulk-edit-select-all"] = () => {
      this.selected = true;
    };
    this.subs["job-bulk-edit-select-none"] = () => {
      this.selected = false;
    };
    this.subs[`job-bulk-edit-select-all-path`] = (path: string) => {
      this.selectIfPath(path);
    };
    this.subs[`job-bulk-edit-select-none-path`] = (path: string) => {
      this.unselectIfPath(path);
    };
    this.subs[`browser-job-item-click:${this.job.id}`] = () => {
      this.toggleSelected();
    };
    this.subs[`browser-job-item-select:${this.job.id}`] = () => {
      this.selected = true;
    };
    Object.keys(this.subs).forEach((key) => {
      //register each listener
      eventBus.on(key, this.subs[key]);
    });
  },
  beforeUnmount() {
    Object.keys(this.subs).forEach((key) => {
      //unregister each listener
      eventBus.off(key, this.subs[key]);
    });
  },
  methods: {
    selectIfPath(path: string) {
      if (
        this.job.groupPath === path ||
        (this.job.groupPath && this.job.groupPath.startsWith(`${path}/`))
      ) {
        this.selected = true;
      }
    },
    unselectIfPath(path: string) {
      if (
        this.job.groupPath === path ||
        (this.job.groupPath && this.job.groupPath.startsWith(`${path}/`))
      ) {
        this.selected = false;
      }
    },
    toggleSelected() {
      if (this.jobPageStore.bulkEditMode) {
        this.selected = !this.selected;
      }
    },
  },
});
</script>

<style scoped lang="scss">
span {
  margin-right: var(--spacing-2);
  padding: 4px 5px;
}
</style>
