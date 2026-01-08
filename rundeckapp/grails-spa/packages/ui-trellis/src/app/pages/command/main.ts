import { createApp, markRaw } from "vue";

import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import NextUiToggle from "../job/browse/NextUiToggle.vue";
import { RootStore } from "../../../library/stores/RootStore";
import { getRundeckContext } from "../../../library";

const rundeckContext = getRundeckContext();
const rootStore = rundeckContext.rootStore;
const eventBus = rundeckContext.eventBus;

function init() {
  // Add NextUiToggle to UI store (similar to adhoc/main.ts pattern)
  rootStore.ui.addItems([
    {
      section: "theme-select",
      location: "after",
      visible: true,
      widget: markRaw(NextUiToggle),
    },
  ]);
}

eventBus.on("ko-adhoc-running", (data: any) => {
  const elm = document.querySelector(
    "#runcontent > .execution-show-log",
  ) as HTMLElement;

  const template = `\
    <LogViewer
        v-if="displayViewer"
        executionId="${data.id}"
        :showSettings="showSettings"
        :config="config"
        ref="viewer"
    />
    `;

  const vue = createApp(
    {
      name: "Command",
      components: { LogViewer },
      props: {
        showSettings: {
          type: Boolean,
          required: true,
        },
        config: {
          type: Object,
          required: true,
        },
      },
      template: template,
      computed: {
        displayViewer() {
          return elm.style.display != "none";
        },
      },
      provide: {
        rootStore,
      },
    },
    {
      showSettings: false,
      config: {
        gutter: true,
        command: false,
        nodeBadge: true,
        timestamps: true,
        stats: false,
      },
    },
  );
  vue.mount(elm);
});

window.addEventListener("DOMContentLoaded", init);
