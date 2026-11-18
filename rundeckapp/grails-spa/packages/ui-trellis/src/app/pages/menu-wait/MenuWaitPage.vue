<template>
  <div class="menu-wait-page">
    <ExportWaitReady
      v-if="showReady"
      :project="project"
      :instance="instance"
      :iproject="iproject"
      :download-url="downloadUrl"
      :return-url="returnUrl"
    />
    <ExportWaitErrors
      :project="project"
      :token="token"
      :instance="instance"
      :iproject="iproject"
      :return-url="returnUrl"
      :not-found="notFound"
      :error-message="errorMessage"
      :errors="errors"
    />
    <ExportWaitProgress
      v-if="showProgress"
      v-model:refresh="refresh"
      :project="project"
      :percentage="percentage"
      :refresh-url="refreshUrl"
    />
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import ExportWaitReady from "../../components/menu-wait/ExportWaitReady.vue";
import ExportWaitErrors from "../../components/menu-wait/ExportWaitErrors.vue";
import ExportWaitProgress from "../../components/menu-wait/ExportWaitProgress.vue";
import {
  pollExportWait,
  EXPORT_WAIT_REQUEST_FAILED,
} from "../../components/menu-wait/services/exportWaitService";
import type { ExportWaitPollResult } from "../../components/menu-wait/types/exportWaitTypes";

const POLL_INTERVAL_MS = 5000;

/**
 * Project export wait page. Shows export progress, polls the server every
 * 5 seconds while auto refresh is enabled, and shows the download link or
 * error panels when the export completes or fails.
 */
export default defineComponent({
  name: "MenuWaitPage",
  components: { ExportWaitReady, ExportWaitErrors, ExportWaitProgress },
  props: {
    token: { type: String, required: true },
    project: { type: String, required: true },
    initialReady: { type: Boolean, default: false },
    initialPercentage: { type: Number, default: 0 },
    instance: { type: String, default: null },
    iproject: { type: String, default: null },
    pollUrl: { type: String, required: true },
    downloadUrl: { type: String, required: true },
    refreshUrl: { type: String, required: true },
    returnUrl: { type: String, required: true },
  },
  data() {
    return {
      ready: this.initialReady,
      percentage: this.initialPercentage,
      notFound: false,
      errorMessage: null as string | null,
      errors: [] as string[],
      refresh: true,
      timer: null as ReturnType<typeof setInterval> | null,
      inFlight: false,
    };
  },
  computed: {
    /**
     * Whether any error field is set.
     * @returns true when a not-found, message or errors list is present
     */
    hasError(): boolean {
      return this.notFound || !!this.errorMessage || this.errors.length > 0;
    },
    /**
     * @returns true when the ready panel should be shown
     */
    showReady(): boolean {
      return this.ready && !this.hasError;
    },
    /**
     * @returns true when the in-progress panel should be shown
     */
    showProgress(): boolean {
      return !this.ready && !this.hasError;
    },
  },
  watch: {
    refresh(newVal: boolean) {
      if (newVal) {
        this.startTimer();
      } else {
        this.stopTimer();
      }
    },
    ready(newVal: boolean) {
      if (newVal) {
        this.applyReadyTitle();
      }
    },
  },
  mounted() {
    if (this.ready) {
      this.applyReadyTitle();
    } else if (this.refresh) {
      this.startTimer();
    }
  },
  unmounted() {
    this.stopTimer();
  },
  methods: {
    /**
     * Set the document title to the ready title.
     */
    applyReadyTitle(): void {
      document.title = this.$t("archive.request.please.wait.pagetitle.ready");
    },
    /**
     * Start the polling interval if it is not already running.
     */
    startTimer(): void {
      if (this.timer || this.ready || this.hasError) {
        return;
      }
      this.timer = setInterval(this.poll, POLL_INTERVAL_MS);
    },
    /**
     * Stop the polling interval.
     */
    stopTimer(): void {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
    },
    /**
     * Fetch the current export state and apply it.
     */
    async poll(): Promise<void> {
      if (this.inFlight) {
        return;
      }
      this.inFlight = true;
      try {
        const result = await pollExportWait(this.pollUrl);
        this.applyResult(result);
      } catch (e) {
        const message = (e as Error)?.message;
        this.errorMessage =
          !message || message === EXPORT_WAIT_REQUEST_FAILED
            ? this.$t("menu.wait.request.failed")
            : message;
        this.refresh = false;
      } finally {
        this.inFlight = false;
      }
    },
    /**
     * Apply a poll result, stopping polling on a terminal state.
     * @param result the poll response
     */
    applyResult(result: ExportWaitPollResult): void {
      if (typeof result.percentage === "number") {
        this.percentage = result.percentage;
      }
      this.notFound = !!result.notFound;
      this.errorMessage = result.errorMessage || null;
      this.errors = Array.isArray(result.errors) ? result.errors : [];
      this.ready = !!result.ready;
      if (this.ready || this.hasError) {
        this.refresh = false;
        this.stopTimer();
      }
    },
  },
});
</script>

<style scoped lang="scss">
.menu-wait-page {
  width: 100%;
}
</style>
