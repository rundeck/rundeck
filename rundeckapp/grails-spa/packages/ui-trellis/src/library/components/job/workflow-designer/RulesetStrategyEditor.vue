<template>
  <div style="display: flex; height: 600px" class="container-fluid">
    <div style="flex-grow: 1">
      <div style="height: 100%">
        <workflow-graph
          v-if="loaded"
          v-model="rulesInput"
          :nodes="nodesData"
          :intersect-root="intersectRoot"
        />
        <slot v-if="!loaded" name="loading">
          <div
            style="
              height: 100%;
              width: 100%;
              background-color: var(--background-color);
              display: flex;
            "
          >
            <div class="loader" style="margin: auto">
              <i class="fas fa-spinner fa-spin loading-spinner text-muted" />
              {{ $t("loading.text") }}
            </div>
          </div>
        </slot>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { getRundeckContext } from "@/library";
import { defineComponent } from "vue";
import WorkflowGraph from "./WorkflowGraph.vue";
import messages from "./i18n";
const localeData = getRundeckContext()?.locale || "en_US";
const lang = getRundeckContext()?.language || "en";
const i18nmessages = {
  [localeData]: Object.assign(
    {},
    messages[localeData] || messages[lang] || messages["en_US"] || {},
  ),
};

interface GraphNode {
  identifier: string;
  label: string;
  title: string;
  icon: {
    class: string;
    image: string;
  };
}
type Callback = (rules: string) => void;

const eventBus = getRundeckContext().eventBus;

/**
 * RulesetStrategyEditor is a custom editor for the RulesetWorkflowStrategy "rules" property.
 */
export default defineComponent({
  name: "RulesetStrategyEditor",
  components: {
    WorkflowGraph,
  },
  inject: ["addUiMessages"],
  props: {
    modelValue: {
      type: String,
      required: true,
    },
  },
  emits: ["update:modelValue"],
  data() {
    return {
      rulesInput: "",
      nodesData: [] as GraphNode[],
      intersectRoot: document.getElementById("layoutBody") || undefined,
      loaded: false,
      updateCallback: null as Callback | null,
    };
  },
  watch: {
    rulesInput: {
      handler(rules: string) {
        if (this.loaded) {
          this.$emit("update:modelValue", rules);
        }
      },
    },
  },
  created() {
    this.addUiMessages([i18nmessages[localeData]]);
    this.rulesInput = this.modelValue;

    eventBus.on("workflow-editor-workflowsteps-updated", async (data: any) => {
      // Load the workflow steps into graph nodes
      if (data.commands) {
        this.nodesData = this.mapCommands(data.commands);
        if (!this.loaded) {
          this.loaded = true;
        }
      }
    });
    // Request the workflow steps from the WorkflowSteps component in case they have already been emitted
    eventBus.emit("workflow-editor-workflowsteps-request");
  },
  methods: {
    /**
     * Map the internal model for workflow steps to graph nodes
     * @param commands
     */
    mapCommands(commands: any[]): GraphNode[] {
      const wfplugins =
        (getRundeckContext().rootStore.plugins.getServicePlugins(
          "WorkflowStep",
        ) as any[]) || [];
      const nodeplugins =
        (getRundeckContext().rootStore.plugins.getServicePlugins(
          "WorkflowNodeStep",
        ) as any[]) || [];
      return commands.map((command: any, i: number) => {
        const type = command.type;
        const plugin = (command.nodeStep ? nodeplugins : wfplugins).find(
          (p) => p.name === type,
        );
        let title = plugin?.title || type;
        let iconCss = "rdicon plugin icon-small icon-med";
        if (plugin?.providerMetadata?.faicon) {
          iconCss = `fas fa-${plugin.providerMetadata.faicon}`;
        }
        if (command.nodeStep && type === "exec-command") {
          title = command.config.adhocRemoteString;
          iconCss = "rdicon shell icon-small icon-med";
        } else if (command.nodeStep && type === "script-file-url") {
          title = this.$t("graph.stepLabel.scriptFile");
          iconCss = "rdicon scriptfile icon-small icon-med";
        } else if (command.nodeStep && type === "script-inline") {
          title = this.$t("graph.stepLabel.script");
          iconCss = "rdicon script icon-small icon-med";
        } else if (command.jobref) {
          title = this.$t("graph.stepLabel.job", [command.jobref.name]);
          iconCss = "glyphicon glyphicon-book";
        }

        return {
          identifier: (Number(i) + 1).toString(),
          label: command.description,
          title,
          icon: {
            class: iconCss,
            image: plugin?.iconUrl,
          },
        };
      });
    },
  },
});
</script>
