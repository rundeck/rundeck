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
            :disabled="!adhocCommandStore?.allowInput"
            autofocus="true"
            @input="handleCommandInput"
            @keypress="handleKeyPress"
          />

          <span class="input-group-btn">
            <button
              class="btn btn-default has_tooltip"
              type="button"
              :title="$t('node.dispatch.settings')"
              data-placement="left"
              data-container="body"
              data-toggle="collapse"
              data-target="#runconfig"
            >
              <i class="glyphicon glyphicon-cog"></i>
            </button>

            <a
              class="btn btn-cta btn-fill runbutton"
              :class="{ disabled: isRunDisabled }"
              :disabled="isRunDisabled"
              @click.prevent="handleSubmit"
            >
              <span v-if="!adhocCommandStore?.running">
                <span v-if="nodeTotal > 0">
                  {{ $t("run.on.count.nodes", [nodeTotal, nodesTitle]) }}
                  <span class="glyphicon glyphicon-play"></span>
                </span>
                <span v-else>No Nodes</span>
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
import { _genUrl } from "../../../library/utilities/genUrl";
import { getAppLinks } from "../../../library";
import { getToken, setToken } from "../../../library/modules/tokens";
import axios from "axios";
import type { PropType } from "vue";

export default defineComponent({
  name: "AdhocCommandForm",
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
  emits: ["submit", "node-total-changed"],
  data() {
    return {
      threadCount: 1,
      nodeKeepgoing: "true",
      running: false,
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
    nodesTitle(): string {
      return this.nodeTotal === 1 ? this.$t("Node") : this.$t("Node.plural");
    },
  },
  mounted() {
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

      // Show loading in output area via EventBus
      if (this.eventBus && typeof this.eventBus.emit === "function") {
        this.eventBus.emit("adhoc-output-loading", "Starting Execution…");
      }

      // Serialize form data
      const form = this.$refs.runboxFormRef as HTMLFormElement;
      if (!form) {
        this.showError("Form not found");
        return false;
      }
      const formData = new FormData(form);
      const data: Record<string, any> = {};
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
        // Get CSRF tokens (matching original _createAjaxSendTokensHandler behavior)
        // Uses X-RUNDECK-TOKEN-KEY and X-RUNDECK-TOKEN-URI headers
        const headers: Record<string, string> = {
          "x-rundeck-ajax": "true",
        };
        
        try {
          // Try to get tokens using the token utility
          const tokenData = await getToken("adhoc_req_tokens");
          if (tokenData && tokenData.TOKEN && tokenData.URI) {
            headers["X-RUNDECK-TOKEN-KEY"] = tokenData.TOKEN;
            headers["X-RUNDECK-TOKEN-URI"] = tokenData.URI;
          }
        } catch (err) {
          // If token not found, try window._rundeck.token as fallback
          if (window._rundeck && window._rundeck.token) {
            headers["X-RUNDECK-TOKEN-KEY"] = window._rundeck.token.TOKEN;
            headers["X-RUNDECK-TOKEN-URI"] = window._rundeck.token.URI;
          }
        }

        const response = await axios.post(
          _genUrl(getAppLinks().scheduledExecutionRunAdhocInline, data),
          data,
          { headers }
        );
        
        // Handle token refresh (matching original _createAjaxReceiveTokensHandler behavior)
        const tokenKey = response.headers["x-rundeck-token-key"];
        const tokenUri = response.headers["x-rundeck-token-uri"];
        if (tokenKey && tokenUri) {
          await setToken(response.headers, "adhoc_req_tokens");
        }

        if (response.data.error) {
          this.showError(response.data.error);
        } else if (!response.data.id) {
          this.showError("Server response was invalid: " + response.data.toString());
        } else {
          // Success - load execution follow fragment (matching original behavior)
          await this.loadExecutionFollow(response.data);
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : String(error);
        this.showError("Request failed: " + errorMessage);
      } finally {
        // Running state will be cleared by ExecutionOutput when execution completes
        // via the adhoc-execution-complete event, which triggers handleExecutionComplete()
      }

      return false;
    },
    async loadExecutionFollow(data: { id: string | number }) {
      // Show loading state via EventBus
      if (this.eventBus && typeof this.eventBus.emit === "function") {
        this.eventBus.emit("adhoc-output-loading", "Loading Output…");
      }

      try {
        const { getAppLinks } = await import("../../../library");
        const { _genUrl } = await import("../../../library/utilities/genUrl");
        const appLinks = getAppLinks();

        const followUrl = _genUrl(appLinks.executionFollowFragment, {
          id: data.id,
          mode: "tail",
        });

        const response = await fetch(followUrl, {
          headers: {
            "x-rundeck-ajax": "true",
          },
        });

        if (response.ok) {
          const html = await response.text();
          
          // Emit content via EventBus
          if (this.eventBus && typeof this.eventBus.emit === "function") {
            this.eventBus.emit("adhoc-output-content", html);
          }
          
          // Wait for Vue to update DOM with content before initializing LogViewer
          // command/main.ts looks for #runcontent > .execution-show-log element
          await this.$nextTick();
          
          // Emit event for LogViewer initialization (command/main.ts listens for this)
          this.eventBus.emit("ko-adhoc-running", data);
          this.$emit("submit", data);
        } else {
          throw new Error(`Failed to load execution output: ${response.statusText}`);
        }
      } catch (err: unknown) {
        const errorMessage = err instanceof Error ? err.message : "Failed to load execution output";
        this.showError(errorMessage);
      }
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

      // Emit error via EventBus
      if (this.eventBus && typeof this.eventBus.emit === "function") {
        this.eventBus.emit("adhoc-output-error", message);
      }
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

