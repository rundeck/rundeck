import { createApp } from "vue";

import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { UiMessage } from "../../../library/stores/UIStore";
import { RootStore } from "../../../library/stores/RootStore";
import { initI18n, commonAddUiMessages } from "../../utilities/i18n";

const eventBus = window._rundeck.eventBus;

const rootStore = new RootStore(window._rundeck.rundeckClient);

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
  vue.provide("addUiMessages", async (messages: UiMessage[]) =>
    commonAddUiMessages(i18n, messages),
  );
  vue.mount(elm);
});
