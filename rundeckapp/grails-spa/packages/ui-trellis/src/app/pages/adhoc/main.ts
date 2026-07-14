import { createApp, markRaw } from "vue";
import VueCookies from "vue-cookies";
import * as uiv from "uiv";
import moment from "moment";

import App from "./App.vue";
import NextUiToggle from "../../../library/components/widgets/settings-bar/NextUIIndicator.vue";
import { initI18n, updateLocaleMessages } from "../../utilities/i18n";
import { getRundeckContext } from "../../../library";
import type { UiMessage } from "../../../library/stores/UIStore";

const rundeckContext = getRundeckContext();
const rootStore = rundeckContext.rootStore;
const eventBus = rundeckContext.eventBus;

const locale = rundeckContext.locale || "en_US";
moment.locale(locale);

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

// Mount Vue app to adhoc-page-vue elements
async function mountAdhocApp() {
  const els = document.body.getElementsByClassName("adhoc-page-vue");

  if (els.length === 0) {
    return;
  }

  for (let i = 0; i < els.length; i++) {
    const el = els[i];

    try {
      const i18n = initI18n();

      const vue = createApp({
        components: { App },
        provide: {
          rootStore,
        },
        template: "<App />",
      });

      // Provide addUiMessages so components can add translations dynamically
      // This matches the pattern used by ui-socket components
      vue.provide("addUiMessages", async (messages: UiMessage[]) => {
        const newMessages = messages.reduce(
          (acc: any, message: UiMessage) =>
            message ? { ...acc, ...message } : acc,
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
            (acc: any, message: UiMessage) =>
              message ? { ...acc, ...message } : acc,
            {},
          ),
        );
      }

      vue.use(uiv);
      vue.use(i18n);
      vue.use(VueCookies);
      vue.mount(el);
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

window.addEventListener("DOMContentLoaded", init);

// Note: LogViewer is now rendered directly in ExecutionOutput.vue component
// No need for separate initialization here
