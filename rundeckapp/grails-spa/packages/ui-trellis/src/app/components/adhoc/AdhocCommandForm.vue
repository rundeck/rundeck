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
                v-if="adhocCommandStore && adhocCommandStore.recentCommandsNoneFound"
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("none") }}
              </li>
              <li
                v-if="adhocCommandStore && !adhocCommandStore.recentCommandsLoaded"
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("loading.text") }}
              </li>
              <li
                v-if="
                  adhocCommandStore &&
                  adhocCommandStore.recentCommandsLoaded &&
                  !adhocCommandStore.recentCommandsNoneFound
                "
                role="presentation"
                class="dropdown-header"
              >
                {{ $t("your.recently.executed.commands") }}
              </li>
              <li
                v-for="(cmd, index) in (adhocCommandStore?.recentCommands || [])"
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
            :value="adhocCommandStore?.commandString || ''"
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
              :aria-label="adhocCommandStore?.running ? $t('running1') : (nodeTotal > 0 ? $t('run.on.count.nodes', [nodeTotal, nodesTitle]) : $t('adhoc.no.nodes.matched'))"
              :aria-disabled="isRunDisabled"
              role="button"
              tabindex="0"
              @click.prevent="handleSubmit"
            >
              <span v-if="!adhocCommandStore?.running">
                <span v-if="nodeTotal > 0">
                  {{ $t("run.on.count.nodes", [nodeTotal, nodesTitle]) }}
                  <span class="glyphicon glyphicon-play" aria-hidden="true"></span>
                </span>
                <span v-else>{{ $t("adhoc.no.nodes.matched") }}</span>
              </span>
              <span v-if="adhocCommandStore?.running">
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
import { AdhocCommandStore } from "../../../library/stores/AdhocCommandStore";
import { NodeFilterStore } from "../../../library/stores/NodeFilterLocalstore";
import { runAdhocCommand } from "./services/adhocService";
import { getAppLinks } from "../../../library";
import UiSocket from "../../../library/components/utils/UiSocket.vue";
import type { PropType } from "vue";

export default defineComponent({
  name: "AdhocCommandForm",
  components: {
    UiSocket,
  },
  props: {
    adhocCommandStore: {
      type: Object as PropType<AdhocCommandStore>,
      required: true,
    },
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
  },
  emits: ["submit", "node-total-changed", "execution-started"],
  data() {
    return {
      threadCount: 1,
      nodeKeepgoing: "true",
      running: false,
      formMounted: false,
    };
  },
  computed: {
    isRunDisabled(): boolean {
      return (
        this.nodeTotal < 1 ||
        !!this.nodeError ||
        !this.adhocCommandStore ||
        this.adhocCommandStore.running ||
        !this.adhocCommandStore.canRun
      );
    },
    isCommandInputDisabled(): boolean {
      // Disable command input when:
      // 1. No nodes matched (nodeTotal < 1)
      // 2. Node filter error exists
      // 3. Execution is running
      // 4. Store doesn't allow input (covers canRun state)
      return (
        this.nodeTotal < 1 ||
        !!this.nodeError ||
        !this.adhocCommandStore ||
        this.adhocCommandStore.running ||
        !this.adhocCommandStore.allowInput
      );
    },
    nodesTitle(): string {
      return this.nodeTotal === 1 ? this.$t("Node") : this.$t("Node.plural");
    },
  },
  mounted() {
    // Mark form as mounted to allow UiSocket to render
    this.$nextTick(() => {
      this.formMounted = true;
    });
    
    // Listen for execution completion
    if (this.eventBus && typeof this.eventBus.on === "function") {
      this.eventBus.on("adhoc-execution-complete", this.handleExecutionComplete);
    }
  },
  beforeUnmount() {
    // Clean up event listeners
    if (this.eventBus && typeof this.eventBus.off === "function") {
      this.eventBus.off("adhoc-execution-complete", this.handleExecutionComplete);
    }
  },
  methods: {
    handleCommandInput(event: Event) {
      const target = event.target as HTMLInputElement;
      this.adhocCommandStore.commandString = target.value;
    },
    handleKeyPress(event: KeyboardEvent) {
      // Submit on Enter key
      if (event.key === "Enter" && !this.isRunDisabled) {
        event.preventDefault();
        this.handleSubmit();
      }
    },
    async loadRecentCommands() {
      if (!this.adhocCommandStore.recentCommandsLoaded) {
        const apiUrl = getAppLinks().adhocHistoryAjax;
        await this.adhocCommandStore.loadRecentCommands(
          apiUrl,
          this.nodeFilterStore
        );
      }
    },
    async handleSubmit() {
      if (this.isRunDisabled || this.running) {
        return false;
      }

      const commandString = this.adhocCommandStore.commandString;
      if (!commandString || !commandString.trim()) {
        return false;
      }

      const filter = this.nodeFilterStore.selectedFilter;
      if (!filter) {
        return false;
      }

      // Set running state
      this.adhocCommandStore.running = true;
      this.running = true;

      // Serialize form data
      const form = this.$refs.runboxFormRef as HTMLFormElement;
      if (!form) {
        this.showError(this.$t("adhoc.form.not.found"));
        return false;
      }
      const formData = new FormData(form);
      const data: Record<string, any> = {};
      const metaFields: Record<string, string> = {};
      
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
      // Clear running state
      this.adhocCommandStore.running = false;
      this.running = false;

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
      this.adhocCommandStore.running = false;
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

