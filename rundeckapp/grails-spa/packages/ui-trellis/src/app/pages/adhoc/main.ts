import { defineComponent, markRaw, createApp } from "vue";
import * as uiv from "uiv";
import moment from "moment";

import App from "./App.vue";
import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { initI18n } from "../../utilities/i18n";
import { getRundeckContext } from "../../../library";

const rundeckContext = getRundeckContext();
const rootStore = rundeckContext.rootStore;
const eventBus = rundeckContext.eventBus;

const locale = rundeckContext.locale || "en_US";
moment.locale(locale);

function init() {
  rundeckContext.rootStore.ui.addItems([
    {
      section: "adhoc-command-page",
      location: "main",
      visible: true,
      widget: markRaw(
        defineComponent({
          components: { App },
          template: `<App />`,
        }),
      ),
    },
  ]);
}
window.addEventListener("DOMContentLoaded", init);

// ============================================================================
// LogViewer Initialization (for both Vue 3 UI and Legacy UI)
// ============================================================================
// This handles LogViewer initialization when execution output is loaded.
// Works for both:
//   - Vue 3 UI: ExecutionOutput.vue loads HTML content, then emits ko-adhoc-running
//   - Legacy UI: adhoc.js loads HTML content via jQuery.load(), then emits ko-adhoc-running
//
// TODO: Once legacy UI is deprecated, the legacy-specific comments below can be removed
//       but the LogViewer initialization logic itself should remain

eventBus.on("ko-adhoc-running", (data: any) => {
  const elm = document.querySelector(
    "#runcontent > .execution-show-log",
  ) as HTMLElement;

  if (!elm) {
    // Element not found, skip initialization
    // This can happen if the execution output HTML hasn't been loaded yet
    return;
  }

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
          // TODO: Once legacy UI is deprecated, this check can be simplified
          //       or removed if we always show the viewer when it's mounted
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

  // Initialize i18n for LogViewer
  const logViewerI18n = initI18n();
  vue.use(uiv);
  vue.use(logViewerI18n);

  vue.mount(elm);
});

