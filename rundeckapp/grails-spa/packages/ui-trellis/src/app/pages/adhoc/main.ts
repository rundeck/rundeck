import { createApp } from "vue";
import VueCookies from "vue-cookies";
import * as uiv from "uiv";
import moment from "moment";

import App from "./App.vue";
import LogViewer from "../../../library/components/execution-log/logViewer.vue";
import { initI18n, updateLocaleMessages } from "../../utilities/i18n";
import { getRundeckContext } from "../../../library";
import type { UiMessage } from "../../../library/stores/UIStore";

const rundeckContext = getRundeckContext();
const rootStore = rundeckContext.rootStore;
const eventBus = rundeckContext.eventBus;

const locale = rundeckContext.locale || "en_US";
moment.locale(locale);

// Mount Vue app to adhoc-page-vue elements
async function mountAdhocApp() {
  const els = document.body.getElementsByClassName("adhoc-page-vue");
  console.log("[adhoc/main.ts] Looking for .adhoc-page-vue elements, found:", els.length);

  if (els.length === 0) {
    // Element not found - might be legacy UI or not ready yet
    console.warn(
      "[adhoc/main.ts] No .adhoc-page-vue elements found. Legacy UI might be active or element not ready.",
    );
    return;
  }

  for (let i = 0; i < els.length; i++) {
    const el = els[i];
    console.log("[adhoc/main.ts] Mounting Vue app to element:", el);

    try {
      const i18n = initI18n();

      const vue = createApp({
        components: { App },
        template: "<App />",
        provide: {
          rootStore,
        },
      });

      // Provide addUiMessages so components can add translations dynamically
      // This matches the pattern used by ui-socket components
      vue.provide("addUiMessages", async (messages: UiMessage[]) => {
        const newMessages = messages.reduce(
          (acc: any, message: UiMessage) => (message ? { ...acc, ...message } : acc),
          {},
        );
        const locale = rundeckContext.locale || "en_US";
        const lang = rundeckContext.language || "en";
        return updateLocaleMessages(i18n, locale, lang, newMessages);
      });

      // Load any initial messages that were already registered in rootStore.ui
      // This ensures translations loaded before Vue app initialization are available
      const initialMessages = rootStore.ui.getUiMessages();
      if (initialMessages.length > 0) {
        const locale = rundeckContext.locale || "en_US";
        const lang = rundeckContext.language || "en";
        await updateLocaleMessages(
          i18n,
          locale,
          lang,
          initialMessages.reduce(
            (acc: any, message: UiMessage) => (message ? { ...acc, ...message } : acc),
            {},
          ),
        );
      }

      vue.use(uiv);
      vue.use(i18n);
      vue.use(VueCookies);
      vue.mount(el);
      console.log("[adhoc/main.ts] Vue app mounted successfully");
    } catch (error) {
      console.error("[adhoc/main.ts] Error mounting Vue app:", error);
    }
  }
}

// Script is loaded with defer, but check if DOM is ready
if (document.readyState === "loading") {
  // DOM not ready yet, wait for it
  document.addEventListener("DOMContentLoaded", mountAdhocApp);
} else {
  // DOM already ready, mount immediately
  mountAdhocApp();
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

