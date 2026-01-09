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
            <button
              v-if="showKillButton"
              class="btn btn-danger btn-xs"
              type="button"
              :disabled="killing"
              @click.stop="handleKillExecution"
            >
              {{ killing ? $t("killing") : $t("kill.job") }}
            </button>
            <a
              v-if="showRetryFailedNodesButton && retryFailedNodesHref"
              :href="retryFailedNodesHref"
              class="btn btn-default btn-xs"
            >
              <i class="glyphicon glyphicon-play"></i>
              {{ $t("retry.failed.nodes") }}&hellip;
            </a>
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
        <LogViewer
          v-else-if="executionId"
          :executionId="executionId"
          :showSettings="false"
          :config="logViewerConfig"
        />
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
// AdhocCommandStore removed - state is now local to AdhocCommandForm
import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { getRundeckContext } from "../../../library";
import { killExecution, getExecutionDetails } from "./services/adhocService";

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
    // adhocCommandStore removed - state is now local to AdhocCommandForm
    // ExecutionOutput communicates completion via event bus only
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
      completionCheckStartTime: null as number | null, // Track when polling started
      maxPollingDuration: 300000, // Maximum 5 minutes of polling (300000ms)
      logViewerConfig: {
        theme: "rundeck",
        gutter: true,
        command: false,
        nodeBadge: true,
        timestamps: true,
        stats: false,
        ansiColor: true,
        lineWrap: true,
      },
      // Execution state data
      executionState: null as string | null,
      executionProject: null as string | null,
      executionNodes: [] as string[],
      executionCompleted: false,
      executionStatusString: null as string | null,
      killing: false,
      failedNodes: [] as string[],
      executionDetailsFetched: false, // Flag to prevent fetching execution details multiple times
      completionEventEmitted: false, // Flag to prevent emitting completion event multiple times
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
    showKillButton(): boolean {
      // Show kill button when:
      // 1. showHeader is true (header is visible)
      // 2. Execution is running (not completed)
      // 3. User has kill permissions (adhocKillAllowed)
      // 4. Execution ID exists
      if (!this.showHeader || !this.executionId) {
        return false;
      }
      const pageParams = this.pageParams as any;
      const adhocKillAllowed = pageParams?.adhocKillAllowed === true;
      const isRunning = !this.executionCompleted && this.executionState?.toUpperCase() === "RUNNING";
      return adhocKillAllowed && isRunning;
    },
    retryFailedNodesHref(): string | null {
      // Generate retry failed nodes URL: /project/{project}/command/run?retryFailedExecId={executionId}
      if (!this.executionId || !this.executionProject) {
        return null;
      }
      const rundeckContext = getRundeckContext();
      const rdBase = (rundeckContext.rdBase || "").replace(/\/$/, ""); // Remove trailing slash
      return `${rdBase}/project/${encodeURIComponent(this.executionProject)}/command/run?retryFailedExecId=${this.executionId}`;
    },
    showRetryFailedNodesButton(): boolean {
      // Show retry failed nodes button when:
      // 1. showHeader is true (header is visible)
      // 2. Execution is completed
      // 3. Execution state is FAILED
      // 4. Execution has failed nodes (fetched via API)
      // 5. Execution ID and project exist
      if (!this.showHeader || !this.executionId || !this.executionProject) {
        return false;
      }
      const isFailed = this.executionCompleted && this.executionState?.toUpperCase() === "FAILED";
      const hasFailedNodes = this.failedNodes && this.failedNodes.length > 0;
      return isFailed && hasFailedNodes;
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
      this.failedNodes = [];
      this.executionDetailsFetched = false;
      this.completionEventEmitted = false;
      }
    },
  },
  mounted() {
    // NOTE: We do NOT listen to our own "adhoc-execution-complete" event here
    // because handleExecutionComplete() is called directly from the polling callback
    // and we don't want to process it twice. The event is emitted for other components
    // (like AdhocCommandForm) to listen to.

    // If executionId is already set (e.g., from prop), start handling it
    if (this.executionId) {
      this.handleExecutionStart(this.executionId);
    }
  },
  beforeUnmount() {
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

      // Reset completion event flag for new execution
      this.completionEventEmitted = false;

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

      // ALWAYS update execution state data (regardless of showHeader)
      // showHeader only controls UI visibility, not state tracking
      this.updateExecutionState(data);
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

      // Track when polling started to prevent infinite loops
      this.completionCheckStartTime = Date.now();

      this.completionCheckInterval = window.setInterval(async () => {
        // CRITICAL: Check if execution already completed BEFORE making any API calls
        // This prevents infinite loops when handleExecutionComplete() has already been called
        if (this.executionDetailsFetched || this.executionCompleted) {
          if (this.completionCheckInterval) {
            clearInterval(this.completionCheckInterval);
            this.completionCheckInterval = null;
          }
          return;
        }
        
        // CRITICAL: Check if we've exceeded maximum polling duration to prevent infinite loops
        // This handles cases where executions get stuck on the server side
        if (this.completionCheckStartTime && (Date.now() - this.completionCheckStartTime) > this.maxPollingDuration) {
          console.warn(`[ExecutionOutput] Polling exceeded maximum duration (${this.maxPollingDuration}ms), stopping to prevent infinite loop`);
          if (this.completionCheckInterval) {
            clearInterval(this.completionCheckInterval);
            this.completionCheckInterval = null;
          }
          // Mark as completed to prevent further polling
          this.executionDetailsFetched = true;
          this.executionCompleted = true;
          this.handleError("Execution polling timeout - execution may be stuck on server");
          return;
        }
        
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
            
            // CRITICAL: Check completion flag again AFTER async fetch (may have changed during fetch)
            if (this.executionDetailsFetched || this.executionCompleted) {
              if (this.completionCheckInterval) {
                clearInterval(this.completionCheckInterval);
                this.completionCheckInterval = null;
              }
              return;
            }
            
            // Check if execution is completed FIRST (before updating state)
            // The original FlowState.update() stops polling when json.completed === true (executionState.js line 245)
            // Also check executionState for ABORTED, FAILED, SUCCEEDED as completion states
            const executionState = data.executionState?.toUpperCase();
            const isCompleted = data.completed === true || 
                               executionState === 'ABORTED' || 
                               executionState === 'FAILED' || 
                               executionState === 'SUCCEEDED';
            
            // ALWAYS update execution state data (regardless of showHeader)
            // showHeader only controls UI visibility, not state tracking
            this.updateExecutionState(data);
            
            if (isCompleted) {
              // Stop polling IMMEDIATELY before any async operations to prevent infinite loop
              if (this.completionCheckInterval) {
                clearInterval(this.completionCheckInterval);
                this.completionCheckInterval = null;
              }
              // Set flags immediately to prevent any other interval callbacks from proceeding
              this.executionDetailsFetched = true;
              this.executionCompleted = true;
              
              // Then handle completion
              this.handleExecutionComplete();
              return; // CRITICAL: Return immediately to prevent any further processing
            }
          }
        } catch (err) {
          // Silently handle errors - execution may still be running
          // But log for debugging
          console.debug("[ExecutionOutput] Error checking execution state:", err);
        }
      }, 1500); // Check every 1.5 seconds (matches original reloadInterval: 1500 for adhoc)
    },
    async handleExecutionComplete() {
      // Prevent multiple calls to this method (guard against infinite loop)
      // NOTE: executionDetailsFetched and executionCompleted are already set to true 
      // before this method is called from the polling callback (line 476-477),
      // so we need to check if we've already processed completion
      // Use a separate flag to track if we've already emitted the event
      if (this.completionEventEmitted) {
        return;
      }
      this.completionEventEmitted = true;

      // Stop polling immediately to prevent infinite loop
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
        this.completionCheckInterval = null;
      }

      // Running state is now managed by AdhocCommandForm via event bus
      // No need to update store here - event will be handled by AdhocCommandForm

      // If execution failed and showHeader is enabled, fetch execution details to get failed nodes
      if (
        this.showHeader &&
        this.executionId &&
        this.executionState?.toUpperCase() === "FAILED"
      ) {
        try {
          const executionDetails = await getExecutionDetails(this.executionId);
          if (executionDetails?.failedNodes && executionDetails.failedNodes.length > 0) {
            this.failedNodes = executionDetails.failedNodes;
          }
        } catch (err) {
          // Silently handle errors - failed nodes list is optional
          console.debug("[ExecutionOutput] Error fetching execution details:", err);
        }
      }

      // Emit completion event for AdhocCommandForm to handle
      // Emit IMMEDIATELY (don't wait for nextTick) so button state updates promptly
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
      // Running state is now managed by AdhocCommandForm via event bus
      // Emit completion event so AdhocCommandForm can reset its state
      if (this.eventBus && typeof this.eventBus.emit === "function") {
        this.eventBus.emit("adhoc-execution-complete", { executionId: this.executionId });
      }
      // Clear completion check interval when output is closed manually
      if (this.completionCheckInterval) {
        clearInterval(this.completionCheckInterval);
        this.completionCheckInterval = null;
      }
    },
    clearError() {
      this.error = null;
    },
    async handleKillExecution() {
      if (!this.executionId || this.killing) {
        return;
      }

      this.killing = true;
      this.error = null;

      try {
        const result = await killExecution(this.executionId, false);
        
        // Check if abort was successful
        if (result.abort?.status === "aborted" || result.abort?.status === "pending") {
          // Execution is being killed, update state
          // The polling will detect the state change and update accordingly
          // Force a state refresh to show the updated status
          await this.waitForExecutionState(this.executionId);
        } else if (result.abort?.status === "failed") {
          // Kill failed
          const reason = result.abort.reason || "Failed to kill execution";
          this.error = reason;
        }
      } catch (err) {
        const errorMessage = err instanceof Error ? err.message : String(err);
        this.error = `Failed to kill execution: ${errorMessage}`;
      } finally {
        this.killing = false;
      }
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

