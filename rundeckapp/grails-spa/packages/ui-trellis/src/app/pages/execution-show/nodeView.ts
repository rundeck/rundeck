import { createApp } from "vue";

import { autorun } from "mobx";

import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { getRundeckContext } from "../../../library";
import * as uiv from "uiv";
import { initI18n, commonAddUiMessages } from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";

const rootStore = getRundeckContext().rootStore;

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

  const div = document.createElement("div");
  const elm = document.querySelector(query)!;
  elm.appendChild(div);

  const template = `\
    <LogViewer
        class="wfnodestep"
        executionId="${execId}"
        node="${node}"
        stepCtx="${stepCtx}"
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
  vue.mount(elm);

  /** Update the KO code when this views output starts showing up */
  const execOutput = rootStore.executionOutputStore.createOrGet(execId);
  autorun((reaction) => {
    const entries = execOutput.getEntriesFiltered(node, stepCtx);

    if (!entries || entries.length == 0) {
      /** There was no output so we update KO and remove the viewer */
      if (execOutput.completed) {
        reaction.dispose();
        nodeStep.outputLineCount(0);
        vue._container!.remove();
        return;
      } else {
        return;
      }
    }

    reaction.dispose();
    nodeStep.outputLineCount(1);
  });
});
