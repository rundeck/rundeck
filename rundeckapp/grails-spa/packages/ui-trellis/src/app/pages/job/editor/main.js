// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import { createApp, markRaw } from "vue";
import VueCookies from "vue-cookies";
import * as uiv from "uiv";
import moment from "moment";

import NotificationsEditorSection from "./NotificationsEditorSection.vue";
import ResourcesEditorSection from "./ResourcesEditorSection.vue";
import SchedulesEditorSection from "./SchedulesEditorSection.vue";
import OtherEditorSection from "./OtherEditorSection.vue";
import { EventBus } from "../../../../library/utilities/vueEventBus";
import { initI18n, updateLocaleMessages } from "../../../utilities/i18n";
import { observer } from "../../../utilities/uiSocketObserver";
import OptionsEditorSection from "./OptionsEditorSection.vue";
import { getRundeckContext } from "@/library";
import { loadJsonData } from "@/app/utilities/loadJsonData";
import NextUiToggle from "@/app/pages/job/browse/NextUiToggle.vue";
import DetailsEditorSection from "@/app/pages/job/editor/DetailsEditorSection.vue";
import ExecutionEditorSection from "./ExecutionEditorSection.vue";

const locale = window._rundeck.locale || "en_US";
moment.locale(locale);

const i18n = initI18n();
const els = document.body.getElementsByClassName(
  "job-editor-notifications-vue",
);

for (let i = 0; i < els.length; i++) {
  const e = els[i];
  const app = createApp({
    name: "JobEditNotificationsApp",
    components: { NotificationsEditorSection },
    data() {
      return { EventBus };
    },
  });
  app.use(uiv);
  app.use(i18n);
  app.use(VueCookies);
  app.mount(e);
}
const resels = document.body.getElementsByClassName("job-editor-resources-vue");

for (let i = 0; i < resels.length; i++) {
  const e = resels[i];
  const rapp = createApp({
    name: "JobEditResourcesApp",
    components: { ResourcesEditorSection },
    data() {
      return { EventBus };
    },
  });
  rapp.use(uiv);
  rapp.use(i18n);
  rapp.provide("addUiMessages", async (messages) => {
    const newMessages = messages.reduce(
      (acc, message) => (message ? { ...acc, ...message } : acc),
      {},
    );
    const locale = window._rundeck.locale || "en_US";
    const lang = window._rundeck.language || "en";
    return updateLocaleMessages(i18n, locale, lang, newMessages);
  });
  rapp.mount(e);
}
const rootStore = getRundeckContext().rootStore;
const uiMeta = loadJsonData("pageUiMeta");
const uiType = uiMeta?.uiType || "current";
rootStore.ui.addItems([
  {
    section: "theme-select",
    location: "after",
    visible: true,
    widget: markRaw(NextUiToggle),
  },
]);
if (uiType === "next") {
  const optsels = document.body.getElementsByClassName(
    "job-editor-options-vue",
  );

  for (let i = 0; i < optsels.length; i++) {
    const e = optsels[i];
    const rapp = createApp({
      name: "JobEditOptionsApp",
      components: { OptionsEditorSection },
    });
    rapp.use(uiv);
    rapp.use(i18n);
    rapp.provide("addUiMessages", async (messages) => {
      const newMessages = messages.reduce(
        (acc, message) => (message ? { ...acc, ...message } : acc),
        {},
      );
      const locale = window._rundeck.locale || "en_US";
      const lang = window._rundeck.language || "en";
      return updateLocaleMessages(i18n, locale, lang, newMessages);
    });
    rapp.mount(e);
  }

  const detailselms = document.body.getElementsByClassName(
    "job-editor-details-vue",
  );

  for (let i = 0; i < detailselms.length; i++) {
    const e = detailselms[i];
    const rapp = createApp({
      name: "JobEditDetailsApp",
      components: { DetailsEditorSection },
    });
    rapp.use(uiv);
    rapp.use(i18n);
    rapp.provide("addUiMessages", async (messages) => {
      const newMessages = messages.reduce(
        (acc, message) => (message ? { ...acc, ...message } : acc),
        {},
      );
      const locale = window._rundeck.locale || "en_US";
      const lang = window._rundeck.language || "en";
      return updateLocaleMessages(i18n, locale, lang, newMessages);
    });
    rapp.mount(e);
  }

  const execsels = document.body.getElementsByClassName(
    "job-editor-execution-vue",
  );

  for (let i = 0; i < execsels.length; i++) {
    const e = execsels[i];
    const rapp = createApp({
      name: "JobEditExecutionApp",
      components: { ExecutionEditorSection },
    });
    rapp.use(uiv);
    rapp.use(i18n);
    rapp.provide("addUiMessages", async (messages) => {
      const newMessages = messages.reduce(
        (acc, message) => (message ? { ...acc, ...message } : acc),
        {},
      );
      const locale = window._rundeck.locale || "en_US";
      const lang = window._rundeck.language || "en";
      return updateLocaleMessages(i18n, locale, lang, newMessages);
    });
    rapp.mount(e);
  }
}

const scsels = document.body.getElementsByClassName("job-editor-schedules-vue");

for (let i = 0; i < scsels.length; i++) {
  const e = scsels[i];
  const sapp = createApp({
    name: "JobEditSchedulesApp",
    components: { SchedulesEditorSection },
    data() {
      return { EventBus };
    },
  });
  sapp.use(uiv);
  sapp.use(i18n);
  sapp.provide("addUiMessages", async (messages) => {
    const newMessages = messages.reduce(
      (acc, message) => (message ? { ...acc, ...message } : acc),
      {},
    );
    const locale = window._rundeck.locale || "en_US";
    const lang = window._rundeck.language || "en";
    return updateLocaleMessages(i18n, locale, lang, newMessages);
  });
  sapp.mount(e);

  const othels = document.body.getElementsByClassName("job-editor-other-vue");

  for (let i = 0; i < othels.length; i++) {
    const e = othels[i];
    const oapp = createApp({
      name: "JobEditOtherApp",
      components: { OtherEditorSection },
      data() {
        return { EventBus };
      },
    });
    oapp.use(uiv);
    oapp.use(i18n);
    oapp.mount(e);
  }
}

//on job edit page listen for dom content changes and install UI Sockets
window.addEventListener("DOMContentLoaded", (event) => {
  // Job Editing page - Workflow Tab

  const elem = document.querySelector("#workflowContent .pflowlist.edit");
  if (elem) {
    observer.observe(elem, { subtree: true, childList: true });
  }
});
