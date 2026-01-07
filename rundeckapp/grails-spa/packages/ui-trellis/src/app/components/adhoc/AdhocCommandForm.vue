<template>
  <div class="col-xs-12">
    <div class="" id="runtab">
      <form
        ref="runboxFormRef"
        id="runbox"
        @submit.prevent="handleSubmit"
        action="adhoc"
        :data-project="project"
      >
        <input type="hidden" name="doNodedispatch" value="true" />

        <!-- Runner selection widget injection point -->
        <!-- This allows rundeckpro's JobRunnerAdhocCommand component to inject hidden fields -->
        <!-- Placed at the top so it appears above the command input (matching UI layout) -->
        <ui-socket
          v-if="formMounted"
          section="adhoc-command-page"
          location="nodefilter"
          :event-bus="eventBus"
        />

        <span class="input-group multiple-control-input-group tight">
          <span class="input-group-btn">
            <button
              type="button"
              class="btn btn-default dropdown-toggle act_adhoc_history_dropdown"
              data-toggle="dropdown"
              @click="loadRecentCommands"
            >
              {{ $t("recent") }} <span class="caret"></span>
            </button>
            <ul class="dropdown-menu">
              <li
                v-if="recentCommandsNoneFound"
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("none") }}
              </li>
              <li
                v-if="!recentCommandsLoaded"
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("loading.text") }}
              </li>
              <li
                v-if="recentCommandsLoaded && !recentCommandsNoneFound"
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("your.recently.executed.commands") }}
              </li>
              <li
                v-for="(cmd, index) in recentCommands"
                :key="index"
              >
                <a
                  href="#"
                  :title="cmd.filter || ''"
                  @click.prevent="cmd.fillCommand()"
                  class="act_fill_cmd"
                >
                  <i
                    class="exec-status icon"
                    :class="cmd.statusClass"
                  ></i>
                  <span>{{ cmd.title }}</span>
                </a>
              </li>
            </ul>
          </span>

          <input
            ref="commandInputRef"
            name="exec"
            size="50"
            type="text"
            :placeholder="$t('enter.a.command')"
            :value="commandString"
            id="runFormExec"
            class="form-control"
            :disabled="isCommandInputDisabled"
            autofocus="true"
            :aria-label="$t('enter.a.command')"
            aria-describedby="command-input-help"
            @input="handleCommandInput"
            @keypress="handleKeyPress"
          />

          <span class="input-group-btn">
            <button
              class="btn btn-default has_tooltip"
              type="button"
              :title="$t('node.dispatch.settings')"
              :aria-label="$t('node.dispatch.settings')"
              data-placement="left"
              data-container="body"
              data-toggle="collapse"
              data-target="#runconfig"
            >
              <i class="glyphicon glyphicon-cog" aria-hidden="true"></i>
            </button>

            <a
              class="btn btn-cta btn-fill runbutton"
              :class="{ disabled: isRunDisabled }"
              :disabled="isRunDisabled"
              :aria-label="running ? $t('running1') : (nodeTotal > 0 ? $t('run.on.count.nodes', [nodeTotal, nodesTitle]) : $t('adhoc.no.nodes.matched'))"
              :aria-disabled="isRunDisabled"
              role="button"
              tabindex="0"
              @click.prevent="handleSubmit"
            >
              <span v-if="!running">
                <span v-if="nodeTotal > 0">
                  {{ $t("run.on.count.nodes", [nodeTotal, nodesTitle]) }}
                  <span class="glyphicon glyphicon-play" aria-hidden="true"></span>
                </span>
                <span v-else>{{ $t("adhoc.no.nodes.matched") }}</span>
              </span>
              <span v-if="running">
                {{ $t("running1") }}
              </span>
            </a>
          </span>
        </span>

        <div class="collapse well well-sm" id="runconfig">
          <div class="form form-inline">
            <h5 style="margin-top: 0">
              {{ $t("node.dispatch.settings") }}
              <div class="pull-right">
                <button
                  class="close"
                  data-toggle="collapse"
                  data-target="#runconfig"
                >
                  &times;
                </button>
              </div>
            </h5>

            <div class="">
              <div
                class="form-group has_tooltip"
                style="margin-top: 6px"
                :title="$t('maximum.number.of.parallel.threads.to.use')"
                data-placement="bottom"
              >
                {{ $t("thread.count") }}
              </div>

              <div class="form-group" style="margin-top: 6px">
                <input
                  min="1"
                  type="number"
                  name="nodeThreadcount"
                  id="runNodeThreadcount"
                  size="2"
                  :placeholder="$t('maximum.threadcount.for.nodes')"
                  class="form-control input-sm"
                  v-model.number="threadCount"
                />
              </div>

              <div class="form-group" style="margin-top: 6px; margin-left: 20px">
                {{ $t("on.node.failure") }}
              </div>

              <div class="form-group">
                <div class="radio">
                  <input
                    type="radio"
                    name="nodeKeepgoing"
                    value="true"
                    id="nodeKeepgoingTrue"
                    v-model="nodeKeepgoing"
                  />
                  <label
                    class="has_tooltip"
                    :title="$t('continue.to.execute.on.other.nodes')"
                    data-placement="bottom"
                    for="nodeKeepgoingTrue"
                  >
                    &nbsp;&nbsp;{{ $t("continue") }}
                  </label>
                </div>
              </div>

              <div class="form-group">
                <div class="radio">
                  <input
                    type="radio"
                    name="nodeKeepgoing"
                    value="false"
                    id="nodeKeepgoingFalse"
                    v-model="nodeKeepgoing"
                  />
                  <label
                    class="has_tooltip"
                    :title="$t('do.not.execute.on.any.other.nodes')"
                    data-placement="bottom"
                    for="nodeKeepgoingFalse"
                  >
                    &nbsp;&nbsp;{{ $t("stop") }}
                  </label>
                </div>
              </div>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import { NodeFilterStore } from "../../../library/stores/NodeFilterLocalstore";
import { runAdhocCommand } from "./services/adhocService";
import { getAppLinks } from "../../../library";
import UiSocket from "../../../library/components/utils/UiSocket.vue";
import type { PropType } from "vue";

// Types for recent commands
interface AdhocLink {
  href: string | null;
  title: string | null;
  execid: string | null;
  filter: string | null;
  extraMetadata: any | null;
  status: string | null;
  succeeded: boolean | null;
  statusClass: string;
}

interface RecentCommand extends AdhocLink {
  fillCommand: () => void;
}

const statusMap: Record<string, string> = {
  scheduled: "running",
  running: "running",
  succeed: "succeed",
  succeeded: "succeed",
  fail: "fail",
  failed: "fail",
  cancel: "aborted",
  aborted: "aborted",
  retry: "failedretry",
  timedout: "timedout",
  timeout: "timedout",
};

function getStatusClass(status: string | null): string {
  if (!status) return "other";
  return statusMap[status] || "other";
}

export default defineComponent({
  name: "AdhocCommandForm",
  components: {
    UiSocket,
  },
  props: {
    nodeFilterStore: {
      type: Object as PropType<NodeFilterStore>,
      required: true,
    },
    project: {
      type: String,
      required: true,
    },
    eventBus: {
      type: Object,
      required: true,
    },
    pageParams: {
      type: Object,
      required: true,
    },
    nodeTotal: {
      type: Number,
      default: 0,
    },
    nodeError: {
      type: String,
      default: null,
    },
    initialCommandString: {
      type: String,
      default: "",
    },
  },
  emits: ["submit", "node-total-changed", "execution-started"],
  data() {
    return {
      // Local reactive state (replaces AdhocCommandStore)
      commandString: this.initialCommandString || "",
      recentCommands: [] as RecentCommand[],
      recentCommandsLoaded: false,
      running: false,
      canRun: false,
      allowInput: true,
      error: null as string | null,
      followControl: null as any,
      loadMax: 20,
      threadCount: 1,
      nodeKeepgoing: "true",
      formMounted: false,
    };
  },
  computed: {
    recentCommandsNoneFound(): boolean {
      return this.recentCommands.length < 1 && this.recentCommandsLoaded;
    },
    isRunDisabled(): boolean {
      return (
        this.nodeTotal < 1 ||
        !!this.nodeError ||
        this.running ||
        !this.canRun
      );
    },
    isCommandInputDisabled(): boolean {
      return (
        this.nodeTotal < 1 ||
        !!this.nodeError ||
        this.running ||
        !this.allowInput
      );
    },
    nodesTitle(): string {
      return this.nodeTotal === 1 ? this.$t("Node") : this.$t("Node.plural");
    },
  },
  watch: {
    nodeTotal() {
      this.updateCanRunAndAllowInput();
    },
    nodeError() {
      this.updateCanRunAndAllowInput();
    },
    running() {
      this.updateCanRunAndAllowInput();
    },
  },
  mounted() {
    // Mark form as mounted to allow UiSocket to render
    this.$nextTick(() => {
      this.formMounted = true;
    });
    
    // Listen for execution completion
    // In Vue 3 Options API, methods automatically have correct 'this' binding
    if (this.eventBus && typeof this.eventBus.on === "function") {
      this.eventBus.on("adhoc-execution-complete", this.handleExecutionComplete);
    }
    
    // Update canRun and allowInput based on initial props
    this.updateCanRunAndAllowInput();
  },
  beforeUnmount() {
    // Clean up event listeners
    if (this.eventBus && typeof this.eventBus.off === "function") {
      this.eventBus.off("adhoc-execution-complete", this.handleExecutionComplete);
    }
    // Stop following execution output if active
    if (this.followControl && typeof this.followControl.stopFollowingOutput === "function") {
      this.followControl.stopFollowingOutput();
    }
  },
  methods: {
    updateCanRunAndAllowInput() {
      if (this.running) {
        this.canRun = false;
        this.allowInput = false;
        return;
      }

      const totalNum = typeof this.nodeTotal === "string" ? parseInt(this.nodeTotal as any, 10) : this.nodeTotal;
      if (totalNum && totalNum !== 0 && this.nodeTotal !== "0" && !this.nodeError) {
        this.canRun = true;
        this.allowInput = true;
      } else {
        this.canRun = false;
        this.allowInput = false;
      }
    },
    handleCommandInput(event: Event) {
      const target = event.target as HTMLInputElement;
      this.commandString = target.value;
    },
    handleKeyPress(event: KeyboardEvent) {
      // Submit on Enter key
      if (event.key === "Enter" && !this.isRunDisabled) {
        event.preventDefault();
        this.handleSubmit();
      }
    },
    async loadRecentCommands() {
      if (!this.recentCommandsLoaded) {
        try {
          this.recentCommandsLoaded = false;
          this.error = null;

          const apiUrl = getAppLinks().adhocHistoryAjax;
          const response = await fetch(`${apiUrl}?max=${this.loadMax}`);
          if (!response.ok) {
            throw new Error(`Request failed: ${response.statusText}`);
          }

          const data = await response.json();
          this.recentCommandsLoaded = true;

          if (data.executions && Array.isArray(data.executions)) {
            this.recentCommands = data.executions.map((exec: any) => {
              const link: RecentCommand = {
                href: exec.href || null,
                title: exec.title || null,
                execid: exec.execid || null,
                filter: exec.filter || null,
                extraMetadata: exec.extraMetadata || null,
                status: exec.status || null,
                succeeded: exec.succeeded || null,
                statusClass: getStatusClass(exec.status),
                fillCommand: () => {
                  this.commandString = exec.title || "";
                  if (this.nodeFilterStore && exec.filter) {
                    this.nodeFilterStore.setSelectedFilter(exec.filter);
                  }
                  // Emit runner-filter-changed event
                  if (exec.extraMetadata) {
                    const runnerEvent = new CustomEvent("runner-filter-changed", {
                      detail: exec.extraMetadata,
                    });
                    window.dispatchEvent(runnerEvent);
                  }
                },
              };
              return link;
            });
          } else {
            this.recentCommands = [];
          }
        } catch (err: any) {
          this.recentCommandsLoaded = true;
          this.error = `Recent commands list: request failed: ${err.message}`;
          console.error("Recent commands list: error receiving data", err);
          this.recentCommands = [];
        }
      }
    },
    async handleSubmit() {
      if (this.isRunDisabled || this.running) {
        return false;
      }

      const commandString = this.commandString;
      if (!commandString || !commandString.trim()) {
        return false;
      }

      const filter = this.nodeFilterStore.selectedFilter;
      if (!filter) {
        return false;
      }

      // Set running state
      this.running = true;

      // Serialize form data
      const form = this.$refs.runboxFormRef as HTMLFormElement;
      if (!form) {
        this.showError(this.$t("adhoc.form.not.found"));
        return false;
      }
      const formData = new FormData(form);
      const data: Record<string, any> = {};
      
      // Extract all form fields - keep meta.* fields as flat fields (not nested)
      // The service will send them as query parameters matching original jQuery.serialize() behavior
      for (const [key, value] of formData.entries()) {
        data[key] = value;
      }

      // Add filter and exclude filter to data (matching original behavior)
      data.filter = filter;
      if (this.nodeFilterStore.selectedExcludeFilter) {
        data.filterExclude = this.nodeFilterStore.selectedExcludeFilter;
      }
      
      // Ensure doNodedispatch is set
      data.doNodedispatch = "true";

      try {
        // Execute adhoc command via service
        // Service will send all fields (including meta.*) as query parameters
        // matching original jQuery.serialize() behavior
        const response = await runAdhocCommand({
          ...data,
          project: this.project,
          // Override fields that need type conversion
          nodeThreadcount: data.nodeThreadcount ? parseInt(data.nodeThreadcount as string, 10) : undefined,
          nodeKeepgoing: data.nodeKeepgoing === "true" || data.nodeKeepgoing === true,
        });

        // Emit execution started event with ID (not ko-adhoc-running)
        // App.vue will pass executionId to ExecutionOutput component
        this.$emit("execution-started", { id: response.id! });
        this.$emit("submit", { id: response.id! });
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : String(error);
        this.showError(this.$t("adhoc.request.failed") + ": " + errorMessage);
      } finally {
        // Running state will be cleared by ExecutionOutput when execution completes
        // via the adhoc-execution-complete event, which triggers handleExecutionComplete()
      }

      return false;
    },
    handleExecutionComplete() {
      console.log("[AdhocCommandForm] handleExecutionComplete called, current running state:", this.running);
      // Clear running state IMMEDIATELY
      this.running = false;
      console.log("[AdhocCommandForm] Running state set to:", this.running);

      // Re-enable command input and focus it (matching original afterRun() behavior)
      this.$nextTick(() => {
        const commandInput = this.$refs.commandInputRef as HTMLInputElement;
        if (commandInput) {
          commandInput.focus();
        }
      });

      // Show rerun button if applicable (handled by existing UI)
      // The original code shows .execRerun elements, but that's handled by the execution output fragment
    },
    showError(message: string) {
      this.running = false;

      // Error will be displayed by ExecutionOutput component if needed
      // No need to emit error event since ExecutionOutput handles its own error state
    },
  },
});
</script>

<style scoped>
/* Styles inherited from existing CSS */
.disabled {
  pointer-events: none;
  opacity: 0.6;
}
</style>
