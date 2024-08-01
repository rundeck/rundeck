import { createApp } from "vue";
import * as uiv from "uiv";
import VueCookies from "vue-cookies";
import PrimeVue from "primevue/config";
// import "primevue/resources/themes/lara-light-amber/theme.css";

import { getRundeckContext } from "../../../library";
import { UiMessage } from "../../../library/stores/UIStore";
import UiSocket from "../../../library/components/utils/UiSocket.vue";
import { initI18n, updateLocaleMessages } from "../../utilities/i18n";

const rootStore = getRundeckContext().rootStore;
const EventBus = getRundeckContext().eventBus;
const closest = function (
  elem: Element,
  className: string,
): HTMLElement | null {
  let parent = elem.parentElement;
  while (parent) {
    if (parent.classList.contains(className)) {
      return parent;
    }
    parent = parent.parentElement;
  }
  return null;
};
window.addEventListener("DOMContentLoaded", () => {
  const elm = document.getElementsByClassName("vue-ui-socket");
  for (const elmElement of elm) {
    if (closest(elmElement, "vue-ui-socket-deferred")) {
      return;
    }
    const eventName = elmElement.getAttribute("vue-socket-on");
    if (eventName) {
      window.addEventListener(eventName, (evt1) =>
        initUiComponentsOnEvent(evt1),
      );
    } else {
      initUiComponents(elmElement);
    }
  }
});

function initUiComponentsOnEvent(evt: Event) {
  const elm = document.getElementsByClassName("vue-ui-socket");
  for (const elmElement of elm) {
    const eventName = elmElement.getAttribute("vue-socket-on");
    if (eventName === evt.type) {
      initUiComponents(elmElement);
    }
  }
}

function initUiComponents(elmElement: any) {
  const i18n = initI18n();
  const vue = createApp({
    components: { UiSocket },
    provide: {
      rootStore,
    },
    data() {
      return {
        EventBus,
      };
    },
  });
  vue.use(VueCookies);
  vue.use(i18n);
  vue.use(uiv);
  vue.use(PrimeVue);

  vue.provide("registerComponent", (name, comp) => {
    vue.component(name, comp);
  });
  vue.provide("addUiMessages", async (messages) => {
    const newMessages = messages.reduce(
      (acc: any, message: UiMessage) =>
        message ? { ...acc, ...message } : acc,
      {},
    );
    const locale = window._rundeck.locale || "en_US";
    const lang = window._rundeck.language || "en";
    return updateLocaleMessages(i18n, locale, lang, newMessages);
  });
  try {
    vue.mount(elmElement);
  } catch (e) {
    console.error("Error mounting vue app", e.message, e);
  }
  rootStore.ui.registerComponent = vue.component;
}
