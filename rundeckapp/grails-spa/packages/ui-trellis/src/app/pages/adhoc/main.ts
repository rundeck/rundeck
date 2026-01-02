// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import { createApp } from "vue";
import VueCookies from "vue-cookies";
import * as uiv from "uiv";
import moment from "moment";

import App from "./App.vue";
import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { EventBus } from "../../../library/utilities/vueEventBus";
import { initI18n } from "../../utilities/i18n";
import { RootStore } from "../../../library/stores/RootStore";

const locale = window._rundeck.locale || "en_US";
moment.locale(locale);

// ============================================================================
// Vue 3 UI Initialization (Vue App Mounting)
// ============================================================================
// TODO: Once legacy UI is deprecated, this section can remain as-is
const els = document.body.getElementsByClassName("adhoc-page-vue");

for (let i = 0; i < els.length; i++) {
  const e = els[i];

  // Create VueI18n instance with options
  const i18n = initI18n();

  const vue = createApp({
    components: { App },
    data() {
      return {
        EventBus: EventBus,
      };
    },
  });
  vue.use(uiv);
  vue.use(i18n);
  vue.use(VueCookies);
  vue.mount(e);
}

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
const eventBus = window._rundeck.eventBus;
const rootStore = new RootStore(window._rundeck.rundeckClient);

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
  vue.mount(elm);
});

