<template>
  <div class="col-sm-12">
    <div
      v-if="error"
      id="runerror"
      class="alert alert-warning collapse show"
    >
      <span class="errormessage">{{ error }}</span>
      <a
        class="close"
        data-toggle="collapse"
        href="#runerror"
        aria-hidden="true"
        @click.prevent="clearError"
      >
        &times;
      </a>
    </div>

    <div
      v-if="showOutput"
      id="runcontent"
      ref="runcontentRef"
      class="card card-modified exec-output card-grey-header nodes_run_content execution-output-content"
      @click="handleCloseOutputClick"
    >
      <div v-if="loading" class="card-content">
        {{ loadingMessage }}
      </div>
      <div
        v-else-if="content"
        v-html="content"
      ></div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { AdhocCommandStore } from "../../../library/stores/AdhocCommandStore";
import type { PropType } from "vue";

export default defineComponent({
  name: "ExecutionOutput",
  props: {
    eventBus: {
      type: Object,
      required: true,
    },
    pageParams: {
      type: Object,
      required: true,
    },
    adhocCommandStore: {
      type: Object as PropType<AdhocCommandStore>,
      required: true,
    },
  },
  data() {
    return {
      executionId: null as string | null,
      showOutput: false,
      error: null as string | null,
      loading: false,
      loadingMessage: "",
      content: "",
      completionCheckInterval: null as number | null,
    };
  },
  mounted() {
    // Listen for execution start event
    if (this.eventBus && typeof this.eventBus.on === "function") {
      this.eventBus.on("ko-adhoc-running", this.handleExecutionStart);
      this.eventBus.on("adhoc-output-loading", this.handleLoading);
      this.eventBus.on("adhoc-output-content", this.handleContent);
      this.eventBus.on("adhoc-output-error", this.handleError);
    }

    // Execution completion detection is handled by startCompletionCheck()
    // which is called when handleExecutionStart() receives execution data
  },
  beforeUnmount() {
    // Clean up event listeners
    if (this.eventBus && typeof this.eventBus.off === "function") {
      this.eventBus.off("ko-adhoc-running", this.handleExecutionStart);
      this.eventBus.off("adhoc-output-loading", this.handleLoading);
      this.eventBus.off("adhoc-output-content", this.handleContent);
      this.eventBus.off("adhoc-output-error", this.handleError);
    }
    
    // Clean up completion check interval
    if (this.completionCheckInterval) {
      clearInterval(this.completionCheckInterval);
      this.completionCheckInterval = null;
    }
  },
  methods: {
    async handleExecutionStart(data: { id: string } | null) {
      if (data && data.id) {
        this.executionId = data.id;
        this.showOutput = true;
        this.error = null;
        this.loading = false;
        this.content = "";

        // Start checking for execution completion
        this.startCompletionCheck(data.id);
      }
    },
    handleLoading(message: string) {
      this.loading = true;
      this.loadingMessage = message || this.$t("loading.text");
      this.showOutput = true;
      this.error = null;
    },
    handleContent(html: string) {
      this.loading = false;
      this.content = html;
      this.showOutput = true;
      this.error = null;
      
      // Note: HTML content is server-sanitized via executionFollowFragment endpoint
      // which uses Rundeck's SanitizedHTMLCodec for security
    },
    handleError(message: string) {
      this.error = message;
      this.loading = false;
      this.showOutput = false;
    },
    startCompletionCheck(executionId: string) {
      // Poll execution state to detect completion
      // This matches the original behavior where nodeflowvm.completed.subscribe() was used
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
      }

      this.completionCheckInterval = setInterval(async () => {
        try {
          const { getAppLinks } = await import("../../../library");
          const { _genUrl } = await import("../../../library/utilities/genUrl");
          const appLinks = getAppLinks();

          // Check execution state
          const stateUrl = _genUrl(appLinks.executionAjaxExecState, { id: executionId });
          const response = await fetch(stateUrl, {
            headers: { "x-rundeck-ajax": "true" },
          });

          if (response.ok) {
            const data = await response.json();
            // Check if execution is completed
            if (data.execstate === "succeeded" || data.execstate === "failed" || 
                data.execstate === "aborted" || data.execstate === "timedout") {
              this.handleExecutionComplete();
            }
          }
        } catch (err) {
          // Silently handle errors - execution may still be running
        }
      }, 2000); // Check every 2 seconds
    },
    handleExecutionComplete() {
      // Stop polling
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
        this.completionCheckInterval = null;
      }

      // Clear running state
      this.adhocCommandStore.running = false;

      // Emit completion event for AdhocCommandForm to handle
      if (this.eventBus && typeof this.eventBus.emit === "function") {
        this.eventBus.emit("adhoc-execution-complete", { executionId: this.executionId });
      }
    },
    handleCloseOutputClick(event: MouseEvent) {
      const target = event.target as HTMLElement;
      // Check if clicked element or its parent has the closeoutput class
      const closeButton = target.closest(".closeoutput");
      if (closeButton) {
        event.preventDefault();
        this.closeOutput();
      }
    },
    closeOutput() {
      this.showOutput = false;
      this.executionId = null;
      this.content = "";
      this.loading = false;
      this.adhocCommandStore.running = false;
      this.adhocCommandStore.stopFollowing();
    },
    clearError() {
      this.error = null;
    },
  },
});
</script>

<style scoped>
.execution-output-content {
  margin-top: 20px;
}
</style>

