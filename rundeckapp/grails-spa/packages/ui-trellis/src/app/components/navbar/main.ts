import { createApp, markRaw, defineComponent } from "vue";

import NavigationBar from "../../../library/components/navbar/NavBar.vue";
import UtilityBar from "../../../library/components/utility-bar/UtilityBar.vue";
import RundeckInfoWidget from "../../../library/components/widgets/rundeck-info/RundeckInfoWidget.vue";
import NextUIIndicator from "../../../library/components/widgets/settings-bar/NextUIIndicator.vue";
import SettingsCogButton from "../../../library/components/widgets/settings-bar/SettingsCogButton.vue";
import SettingsModal from "../../../library/components/widgets/settings-bar/SettingsModal.vue";

import { UtilityBarItem } from "../../../library/stores/UtilityBar";
import { getRundeckContext, getAppLinks } from "../../../library";
import { commonAddUiMessages, initI18n } from "../../utilities/i18n";
import * as uiv from "uiv";
import VueCookies from "vue-cookies";

const appLinks = getAppLinks();
const rootStore = getRundeckContext().rootStore;

window.addEventListener("DOMContentLoaded", initNav);
window.addEventListener("DOMContentLoaded", initUtil);

/** Do not wait for document to load before adding items */
rootStore.utilityBar.addItems([
  {
    type: "widget",
    id: "utility-edition",
    container: "root",
    group: "left",
    class: rootStore.system.appInfo.logocss + " app-logo",
    label: rootStore.system.appInfo.title.toUpperCase(),
    visible: true,
    widget: markRaw({
      name: "RundeckInfoWidgetItem",
      components: { RundeckInfoWidget },
      template: `<RundeckInfoWidget/>`,
      provide: {
        rootStore,
      },
    }),
  },
  {
    type: "widget",
    id: "utility-nextui-indicator",
    container: "root",
    group: "right",
    visible: true,
    inline: true,
    order: 200,
    widget: markRaw(
      defineComponent({
        name: "NextUIIndicatorWidget",
        components: { NextUIIndicator },
        provide: {
          rootStore,
        },
        template: `<NextUIIndicator />`,
      }),
    ),
  },
  {
    type: "widget",
    id: "utility-settings-cog",
    container: "root",
    group: "right",
    visible: true,
    inline: true,
    order: 100,
    widget: markRaw(
      defineComponent({
        name: "SettingsCogButtonWidget",
        components: { SettingsCogButton },
        provide: {
          rootStore,
        },
        template: `<SettingsCogButton />`,
      }),
    ),
  },
  {
    type: "widget",
    id: "utility-settings-modal",
    container: "root",
    group: "right",
    visible: true,
    inline: true,
    order: 300,
    widget: markRaw(
      defineComponent({
        name: "SettingsModalWidget",
        components: { SettingsModal },
        provide: {
          rootStore,
        },
        template: `<SettingsModal />`,
      }),
    ),
  },
] as Array<UtilityBarItem>);

function initNav() {
  const elm = document.getElementById("section-navbar") as HTMLElement;

  const vue = createApp({
    name: "NavigationBarApp",
    components: { NavigationBar },
    provide: {
      rootStore,
    },
    template: `<NavigationBar />`,
  });
  vue.mount(elm);
}

function initUtil() {
  const elm = document.getElementById("utilityBar") as HTMLElement;

  const vue = createApp({
    name: "UtilityBarApp",
    components: { UtilityBar },
    provide: {
      rootStore,
    },
    template: `<UtilityBar />`,
  });

  const i18n = initI18n();
  vue.use(VueCookies);
  vue.use(i18n);
  vue.use(uiv);
  vue.provide("addUiMessages", async (messages) => {
    await commonAddUiMessages(i18n, messages);
  });
  vue.mount(elm);
}
