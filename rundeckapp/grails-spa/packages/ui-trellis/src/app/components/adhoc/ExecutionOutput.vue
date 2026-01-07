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
      <!-- Card Header with Execution Info (only if showHeader prop is true) -->
      <div v-if="showHeader && executionId" class="card-header">
        <div class="row">
          <div class="col-xs-12 col-sm-6">
            <i
              class="exec-status icon"
              :data-execstate="executionState"
              :data-statusstring="executionStatusString"
            ></i>
            <a
              v-if="executionHref"
              :href="executionHref"
              class="execution-id-link"
            >
              #{{ executionId }}
            </a>
            <span
              v-if="executionStatusString"
              class="execution-status-text"
              :class="statusTextClass"
            >
              {{ executionStatusString }}
            </span>
          </div>
          <div class="col-xs-12 col-sm-6 text-right execution-action-links">
            <a
              v-if="executionCompleted && executionProject && saveAsJobHref"
              :href="saveAsJobHref"
              class="btn btn-default btn-xs"
            >
              {{ $t("execution.action.saveAsJob") }}
            </a>
            <button
              class="btn btn-link closeoutput"
              type="button"
              @click="closeOutput"
            >
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Original logic when showHeader is false -->
      <template v-if="!showHeader">
        <div v-if="loading" class="card-content">
          {{ loadingMessage }}
        </div>
        <div
          v-else-if="content"
          v-html="content"
        ></div>
      </template>

      <!-- New logic with header when showHeader is true -->
      <div v-else class="card-content">
        <div v-if="loading">
          {{ loadingMessage }}
        </div>
        <LogViewer
          v-else-if="executionId"
          :executionId="executionId"
          :showSettings="false"
          :config="logViewerConfig"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { AdhocCommandStore } from "../../../library/stores/AdhocCommandStore";
import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import type { PropType } from "vue";
import { getRundeckContext } from "../../../library";

export default defineComponent({
  name: "ExecutionOutput",
  components: {
    LogViewer,
  },
  props: {
    executionId: {
      type: String,
      default: null,
    },
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
    showHeader: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      showOutput: false,
      error: null as string | null,
      loading: false,
      loadingMessage: "",
      completionCheckInterval: null as number | null,
      logViewerConfig: {
        gutter: true,
        command: false,
        nodeBadge: true,
        timestamps: true,
        stats: false,
      },
      // Execution state data
      executionState: null as string | null,
      executionProject: null as string | null,
      executionNodes: [] as string[],
      executionCompleted: false,
      executionStatusString: null as string | null,
    };
  },
  computed: {
    executionHref(): string | null {
      if (!this.executionId || !this.executionProject) {
        return null;
      }
      const rundeckContext = getRundeckContext();
      const rdBase = rundeckContext.rdBase || "";
      return `${rdBase}/execution/show/${this.executionId}?project=${encodeURIComponent(this.executionProject)}`;
    },
    saveAsJobHref(): string | null {
      if (!this.executionId || !this.executionProject) {
        return null;
      }
      const rundeckContext = getRundeckContext();
      const rdBase = rundeckContext.rdBase || "";
      return `${rdBase}/scheduledExecution/createFromExecution?executionId=${this.executionId}&project=${encodeURIComponent(this.executionProject)}`;
    },
    statusTextClass(): string {
      if (!this.executionState) {
        return "";
      }
      const state = this.executionState.toUpperCase();
      if (state === "FAILED" || state === "ABORTED") {
        return "text-danger";
      }
      if (state === "SUCCEEDED") {
        return "text-success";
      }
      if (state === "RUNNING") {
        return "text-info";
      }
      return "";
    },
  },
  watch: {
    executionId(newId: string | null) {
      if (newId) {
        this.handleExecutionStart(newId);
      } else {
      // Reset state when executionId is cleared
      this.showOutput = false;
      this.loading = false;
      this.error = null;
      // Reset execution state data
      this.executionState = null;
      this.executionProject = null;
      this.executionNodes = [];
      this.executionCompleted = false;
      this.executionStatusString = null;
      }
    },
  },
  mounted() {
    // Listen for execution completion event
    if (this.eventBus && typeof this.eventBus.on === "function") {
      this.eventBus.on("adhoc-execution-complete", this.handleExecutionComplete);
    }

    // If executionId is already set (e.g., from prop), start handling it
    if (this.executionId) {
      this.handleExecutionStart(this.executionId);
    }
  },
  beforeUnmount() {
    // Clean up event listeners
    if (this.eventBus && typeof this.eventBus.off === "function") {
      this.eventBus.off("adhoc-execution-complete", this.handleExecutionComplete);
    }
    
    // Clean up completion check interval
    if (this.completionCheckInterval) {
      clearInterval(this.completionCheckInterval);
      this.completionCheckInterval = null;
    }
  },
  methods: {
    async handleExecutionStart(executionId: string) {
      if (!executionId) {
        return;
      }

      this.showOutput = true;
      this.error = null;
      this.loading = true;
      this.loadingMessage = this.$t("adhoc.loading.execution.output") || "Loading Execution Output…";

      try {
        // Wait for first state response to confirm execution exists
        await this.waitForExecutionState(executionId);
        
        // Start polling for completion (ajaxExecState returns JSON, not HTML)
        this.startCompletionCheck(executionId);
        
        // Hide loading state - LogViewer will handle its own loading
        this.loading = false;
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : String(err);
        this.handleError(errorMessage);
      }
    },
    
    async waitForExecutionState(executionId: string) {
      // Poll once to confirm execution exists and fetch execution details
      const { getAppLinks } = await import("../../../library");
      const { _genUrl } = await import("../../../library/utilities/genUrl");
      const appLinks = getAppLinks();
      const stateUrl = _genUrl(appLinks.executionAjaxExecState, { id: executionId });
      
      const response = await fetch(stateUrl, {
        headers: { "x-rundeck-ajax": "true" },
      });
      
      if (!response.ok) {
        throw new Error(`Failed to load execution state: ${response.statusText}`);
      }
      
      const data = await response.json();
      if (data.error) {
        throw new Error(data.error);
      }

      // Store execution state data only if showHeader is enabled
      if (this.showHeader) {
        this.updateExecutionState(data);
      }
    },
    updateExecutionState(data: any) {
      // Update execution state from ajaxExecState response
      this.executionState = data.executionState || null;
      this.executionCompleted = data.completed === true;
      this.executionNodes = data.allNodes || data.targetNodes || [];
      this.executionStatusString = this.getExecutionStatusString(data.executionState);
      
      // Get project from pageParams or try to extract from data
      if (this.pageParams && (this.pageParams as any).project) {
        this.executionProject = (this.pageParams as any).project;
      } else if (data.project) {
        this.executionProject = data.project;
      }
    },
    getExecutionStatusString(state: string | null | undefined): string {
      if (!state) {
        return "";
      }
      const stateUpper = state.toUpperCase();
      const statusMap: Record<string, string> = {
        SUCCEEDED: this.$t("execution.status.succeeded") || "Succeeded",
        FAILED: this.$t("execution.status.failed") || "Failed",
        RUNNING: this.$t("execution.status.running") || "Running",
        ABORTED: this.$t("execution.status.aborted") || "Aborted",
        WAITING: this.$t("execution.status.waiting") || "Waiting",
      };
      return statusMap[stateUpper] || state;
    },
    
    handleError(message: string) {
      this.error = message;
      this.loading = false;
      this.showOutput = false;
    },
    startCompletionCheck(executionId: string) {
      // Poll execution state to detect completion
      // This matches the original FlowState behavior from executionState.js
      // The executionAjaxExecState endpoint returns JSON: {completed: true/false, executionState: "...", ...}
      // We stop polling when completed === true (matching executionState.js line 245)
      // This endpoint is used ONLY for completion detection, NOT for HTML loading
      
      // Clear any existing interval first
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
        this.completionCheckInterval = null;
      }

      // Only start if we have a valid executionId
      if (!executionId) {
        return;
      }

      this.completionCheckInterval = window.setInterval(async () => {
        // Double-check interval still exists (may have been cleared)
        if (!this.completionCheckInterval) {
          return;
        }
        
        // Verify executionId matches (prevent checking wrong execution)
        if (this.executionId !== executionId) {
          if (this.completionCheckInterval) {
            clearInterval(this.completionCheckInterval);
            this.completionCheckInterval = null;
          }
          return;
        }

        try {
          const { getAppLinks } = await import("../../../library");
          const { _genUrl } = await import("../../../library/utilities/genUrl");
          const appLinks = getAppLinks();

          // Check execution state (matches original FlowState.callUpdate() behavior)
          // ajaxExecState returns JSON: {completed: true/false, executionState: "...", ...}
          const stateUrl = _genUrl(appLinks.executionAjaxExecState, { id: executionId });
          const response = await fetch(stateUrl, {
            headers: { "x-rundeck-ajax": "true" },
          });

          if (response.ok) {
            const data = await response.json();
            // Update execution state data only if showHeader is enabled
            if (this.showHeader) {
              this.updateExecutionState(data);
            }
            // Check if execution is completed
            // The original FlowState.update() stops polling when json.completed === true (executionState.js line 245)
            // This matches the behavior where nodeflowvm.completed becomes true
            if (data.completed === true) {
              this.handleExecutionComplete();
            }
          }
        } catch (err) {
          // Silently handle errors - execution may still be running
          // But log for debugging
          console.debug("[ExecutionOutput] Error checking execution state:", err);
        }
      }, 1500); // Check every 1.5 seconds (matches original reloadInterval: 1500 for adhoc)
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
      this.loading = false;
      this.adhocCommandStore.running = false;
      this.adhocCommandStore.stopFollowing();
      // Clear completion check interval when output is closed manually
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
        this.completionCheckInterval = null;
      }
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

.card-header {
  padding: 10px 15px;
  border-bottom: 1px solid #ddd;
  background-color: #f5f5f5;
}

.card-header .row {
  margin: 0;
  display: flex;
  align-items: center;
  min-height: 34px;
}

.card-header .col-xs-12.col-sm-6 {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.card-header .col-xs-12.col-sm-6:first-child {
  gap: 4px;
}

.exec-status.icon {
  margin-right: 0;
  display: inline-block;
  vertical-align: middle;
}

.execution-id-link {
  font-weight: bold;
  color: #337ab7;
  text-decoration: none;
  margin-left: 0;
  display: inline-block;
  vertical-align: middle;
}

.execution-id-link:hover {
  text-decoration: underline;
  color: #23527c;
}

.execution-status-text {
  font-weight: 500;
  margin-left: 0;
  display: inline-block;
  vertical-align: middle;
}

.execution-action-links {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.execution-action-links .btn-default.btn-xs {
  margin-right: 0;
}

.execution-action-links .btn-link {
  padding: 0;
  font-size: 18px;
  line-height: 1;
  color: #999;
  text-decoration: none;
  border: none;
  background: none;
  cursor: pointer;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 8px;
}

.execution-action-links .btn-link:hover {
  color: #333;
}

.card-content {
  padding: 15px;
}
</style>

