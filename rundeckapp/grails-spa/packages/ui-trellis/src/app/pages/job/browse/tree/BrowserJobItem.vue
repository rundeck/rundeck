<template>
  <div
    ref="itemDiv"
    class="job-list-row-item hover-reveal-hidden"
    @click="handleClick"
  >
    <ui-socket
      :key="'before/' + job.id"
      section="job-browse-item"
      location="before-job-name"
      :socket-data="{ job }"
    />
    <ui-socket
      v-for="meta in job.meta"
      :key="'before/' + job.id + '/' + meta.name"
      :location="`before-job-name:meta:${meta.name}`"
      section="job-browse-item"
      :socket-data="{ job, meta: meta.data }"
    />
    <a
      v-if="showJobsAsLinks"
      :href="jobLinkHref(job)"
      :data-job-id="job.uuid"
      class="link-quiet job-name"
      :title="job.jobName"
    >
      {{ job.jobName }}
    </a>
    <button
      v-else
      class="btn-link link-quiet"
      :data-job-id="job.uuid"
      @click.prevent="handleJobSelection"
    >
      {{ job.jobName }}
    </button>
    <span
      v-if="job.description"
      class="text-secondary job-description"
      :title="job.description"
    >
      {{ shortDescription }}
    </span>
    <ui-socket
      v-for="meta in job.meta"
      :key="'after/' + job.id + '/' + meta.name"
      :location="`after-job-name:meta:${meta.name}`"
      section="job-browse-item"
      :socket-data="{ job, meta: meta.data }"
    />
    <ui-socket
      :key="'after/' + job.id"
      location="after-job-name"
      section="job-browse-item"
      :socket-data="{ job }"
    />
  </div>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import UiSocket from "@/library/components/utils/UiSocket.vue";
import {
  JobPageStore,
  JobPageStoreInjectionKey,
} from "@/library/stores/JobPageStore";
import { JobBrowseItem } from "@/library/types/jobs/JobBrowse";
import { defineComponent, PropType, inject } from "vue";

const context = getRundeckContext();
const eventBus = context.eventBus;
export default defineComponent({
  name: "BrowserJobItem",
  components: { UiSocket },
  inject: {
    showJobsAsLinks: {
      type: Boolean,
      default: true,
    },
  },
  props: {
    job: {
      type: Object as PropType<JobBrowseItem>,
      required: true,
    },
    loadMeta: {
      type: Boolean,
      default: false,
    },
  },
  setup() {
    return {
      jobPageStore: inject(JobPageStoreInjectionKey) as JobPageStore,
    };
  },
  computed: {
    shortDescription() {
      if (!this.job.description) {
        return "";
      }
      if (this.job.description.indexOf("\n") > -1) {
        return this.job.description.split("\n")[0];
      }
      return this.job.description;
    },
  },
  methods: {
    jobLinkHref(job: JobBrowseItem) {
      return `${context.rdBase}project/${context.projectName}/job/show/${job.id}`;
    },
    /**
     * Check if the element has a parent with a nodeName in the list
     * @param el element
     * @param top stop at this ancestor
     * @param nodeTypes list of node names to check
     */
    hasDomParent(el: HTMLElement, top: HTMLElement, nodeTypes: string[]) {
      while (top !== el) {
        if (nodeTypes.indexOf(el.nodeName) > -1) {
          return true;
        }
        el = el.parentElement as HTMLElement;
      }
      return false;
    },
    async handleClick(event) {
      if (
        !this.hasDomParent(event.target, this.$refs.itemDiv, [
          "INPUT",
          "BUTTON",
          "A",
        ])
      ) {
        //only emit if the click was not within a button,input or link
        eventBus.emit(`browser-job-item-click:${this.job.id}`, this.job);
        if (this.loadMeta && !this.job.meta) {
          this.job.meta = await this.jobPageStore
            .getJobBrowser()
            .loadJobMeta(this.job.id);
        }
      }
    },
    async handleJobSelection() {
      eventBus.emit(`browser-job-item-selection`, this.job);
    },
  },
});
</script>

<style scoped lang="scss">
.job-list-row-item {
  padding: 1px 0 1px 3px;
  border-style: solid;
  border-width: 0 0 3px 0;
  border-color: transparent;
  cursor: pointer;
  display: flex;
  flex-wrap: nowrap;
  gap: var(--spacing-2);
  .job-name,
  .job-description {
    flex: 1 2 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .job-name:has(+ .job-description) {
    flex: 0 1 auto;
  }
  .btn.btn-xs {
    margin-right: 0;
  }
  .job-actions {
    margin-left: auto;
    flex: 0 0 auto;
  }
}
.hover .job-list-row-item {
  background: var(--background-color-accent-lvl2);
  border-color: var(--border-color);
}

.btn-link.link-quiet {
  color: var(--font-color);

  &:hover,
  &:focus {
    color: var(--primary-highlight-color);
  }
}
</style>
