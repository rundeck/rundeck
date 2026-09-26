<template>
  <span class="job-breadcrumbs">
    <span class="job-breadcrumb-item">
      <a :href="browseHref('')" @click.prevent="browsePath('')">
        <slot name="root">&larr;</slot>
      </a>
    </span>
    <template v-for="(part, i) in parts">
      <span v-if="i != 0" class="breadcrumb-separator job-breadcrumb-item">
        <slot name="separator">/</slot>
      </span>

      <a
        :class="linkCss"
        class="job-breadcrumb-item"
        :title="$t('view.jobs.in.this.group')"
        :href="browseHref(subgroup(i))"
        @click.prevent="browsePath(subgroup(i))"
      >
        <b v-if="i == 0" class="glyphicon glyphicon-folder-close"></b>

        {{ part }}
      </a>
    </template>
  </span>
</template>

<script lang="ts">
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { defineComponent, inject } from "vue";

export default defineComponent({
  name: "JobGroupsBreadcrumbs",
  props: {
    groupPath: String,
    linkCss: String,
  },
  emits: ["browseTo"],

  setup(props) {
    const jobPageStore: JobPageStore = inject(
      JobPageStoreInjectionKey,
    ) as JobPageStore;
    return {
      jobPageStore,
    };
  },
  computed: {
    parts() {
      return this.groupPath.split("/");
    },
  },
  methods: {
    subgroup(i: number) {
      return this.parts.slice(0, i + 1).join("/");
    },
    browseHref(path: string) {
      return this.jobPageStore.jobPagePathHref(path);
    },
    browsePath(path: string) {
      this.$emit("browseTo", path, this.browseHref(path));
    },
  },
});
</script>

<style scoped lang="scss">
.breadcrumb-separator {
  color: var(--text-secondary-color);
}

.job-breadcrumbs {
  .job-breadcrumb-item {
    margin-right: var(--spacing-4);
  }
}
</style>
