import { createApp, markRaw } from "vue";

import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import NextUiToggle from "../../../library/components/widgets/settings-bar/NextUIIndicator.vue";
import { RootStore } from "../../../library/stores/RootStore";
import {
  initI18n,
  commonAddUiMessages,
  type LocalizedMessages,
} from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";
import { getRundeckContext } from "../../../library";

const rundeckContext = getRundeckContext();
const rootStore = rundeckContext.rootStore;
const eventBus = rundeckContext.eventBus;

function init() {
  // Add NextUIIndicator to UI store
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

  const i18n = initI18n();

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
  vue.use(i18n);
  vue.provide(
    "addUiMessages",
    async (messages: UiMessage[] | LocalizedMessages) =>
      commonAddUiMessages(i18n, messages),
  );
  vue.mount(elm);
});

window.addEventListener("DOMContentLoaded", init);
