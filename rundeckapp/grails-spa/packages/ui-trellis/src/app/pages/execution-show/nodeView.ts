import { createApp } from "vue";

import { autorun } from "mobx";

import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { getRundeckContext } from "../../../library";
import * as uiv from "uiv";
import { initI18n, commonAddUiMessages } from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";

const rootStore = getRundeckContext().rootStore;

const mountedNodeApps = new Map<string, ReturnType<typeof createApp>>();

window._rundeck.eventBus.on("ko-exec-show-output", (nodeStep: any) => {
  const execId = nodeStep.flow.executionId();
  const stepCtx = nodeStep.stepctx;
  const node = nodeStep.node.name;

  const query = [
    `.wfnodeoutput[data-node="${nodeStep.node.name}"]`,
    stepCtx ? `[data-stepctx="${stepCtx}"]` : undefined,
  ]
    .filter((e) => e)
    .join("");

  const elm = document.querySelector(query) as HTMLElement;

  // The wfnodeoutput element has no explicit height in the Knockout template.
  // The LogViewer's entire height:100% chain (execution-log → scroller → node-chunk
  // → DynamicScroller) collapses to auto without a concrete parent height.
  // Without a fixed height, vue-virtual-scroller cannot measure its viewport and
  // attempts to render all items at once, crashing when item count exceeds its limit.
  elm.style.height = "600px";
  elm.style.overflow = "hidden";

  // Unmount any existing viewer for this node/step to avoid stacking
  // multiple LogViewer instances on the same container.
  const appKey = `${execId}::${node}::${stepCtx}`;
  const existingApp = mountedNodeApps.get(appKey);
  if (existingApp) {
    existingApp.unmount();
    mountedNodeApps.delete(appKey);
  }

  const template = `\
    <LogViewer
        class="wfnodestep"
        executionId="${execId}"
        node="${node}"
        ${stepCtx ? `stepCtx="${stepCtx}"` : ""}
        :showSettings="false"
        ref="viewer"
        :config="config"
    />
    `;

  const i18n = initI18n();

  const vue = createApp({
    name: "NodeLogViewerApp",
    components: { LogViewer },
    provide: {
      rootStore,
    },
    props: {
      config: {
        default: () => ({
          gutter: true,
          command: false,
          nodeBadge: false,
          timestamps: true,
          stats: false,
        }),
      },
    },
    template: template,
  });
  vue.use(uiv);
  vue.use(i18n);
  vue.provide("addUiMessages", async (messages: UiMessage[]) =>
    commonAddUiMessages(i18n, messages),
  );
  // Mount on elm directly (preserving its CSS height/overflow for the virtual scroller)
  vue.mount(elm);
  mountedNodeApps.set(appKey, vue);

  /** Update the KO code when this views output starts showing up */
  const execOutput = rootStore.executionOutputStore.createOrGet(execId);
  autorun((reaction) => {
    const entries = execOutput.getEntriesFiltered(node, stepCtx);

    if (!entries || entries.length == 0) {
      /** There was no output so we update KO and remove the viewer */
      if (execOutput.completed) {
        reaction.dispose();
        nodeStep.outputLineCount(0);
        // Unmount the app and restore the elm container so Knockout can reuse it.
        // Do NOT call vue._container!.remove() since _container is elm itself
        // (.wfnodeoutput), and removing it from the DOM breaks subsequent
        // expand/collapse cycles in the nodes view.
        vue.unmount();
        mountedNodeApps.delete(appKey);
        elm.style.height = "";
        elm.style.overflow = "";
        return;
      } else {
        return;
      }
    }

    reaction.dispose();
    nodeStep.outputLineCount(1);
  });
});
