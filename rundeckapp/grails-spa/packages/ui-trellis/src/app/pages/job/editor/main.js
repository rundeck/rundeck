// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import { createApp, markRaw } from "vue";
import { createPinia } from "pinia";
import VueCookies from "vue-cookies";
import * as uiv from "uiv";
import moment from "moment";

import NotificationsEditorSection from "./NotificationsEditorSection.vue";
import ResourcesEditorSection from "./ResourcesEditorSection.vue";
import SchedulesEditorSection from "./SchedulesEditorSection.vue";
import OtherEditorSection from "./OtherEditorSection.vue";
import { initI18n, updateLocaleMessages } from "../../../utilities/i18n";
import { observer } from "../../../utilities/uiSocketObserver";
import OptionsEditorSection from "./OptionsEditorSection.vue";
import { getRundeckContext } from "@/library";
import { loadJsonData } from "@/app/utilities/loadJsonData";
import NextUiToggle from "@/app/pages/job/browse/NextUiToggle.vue";
import DetailsEditorSection from "@/app/pages/job/editor/DetailsEditorSection.vue";
import ExecutionEditorSection from "./ExecutionEditorSection.vue";
import WorkflowEditorSection from "@/app/pages/job/editor/WorkflowEditorSection.vue";

const locale = window._rundeck.locale || "en_US";
moment.locale(locale);

const i18n = initI18n();
const EventBus = getRundeckContext().eventBus;
const rootStore = getRundeckContext().rootStore;
const uiMeta = loadJsonData("pageUiMeta");
const uiType = uiMeta?.uiType || "current";
const pinia = createPinia();

const jobSections = [
  {
    name: "JobEditDetailsApp",
    component: { DetailsEditorSection },
    elementClass: "job-editor-details-vue",
    visible: uiType === "next",
  },
  {
    name: "JobEditNotificationsApp",
    component: { NotificationsEditorSection },
    elementClass: "job-editor-notifications-vue",
    addEventBus: true,
    addCookies: true,
    visible: true,
  },
  {
    name: "JobEditResourcesApp",
    component: { ResourcesEditorSection },
    elementClass: "job-editor-resources-vue",
    addEventBus: true,
    addUiMessages: true,
    visible: true,
  },
  {
    name: "JobEditExecutionApp",
    component: { ExecutionEditorSection },
    elementClass: "job-editor-execution-vue",
    visible: true,
  },
  {
    name: "JobEditSchedulesApp",
    component: { SchedulesEditorSection },
    elementClass: "job-editor-schedules-vue",
    addEventBus: true,
    addUiMessages: true,
    visible: true,
  },
  {
    name: "JobEditOtherApp",
    component: { OtherEditorSection },
    elementClass: "job-editor-other-vue",
    addEventBus: true,
    visible: true,
  },
  {
    name: "JobEditWorkflowApp",
    component: { WorkflowEditorSection },
    elementClass: "job-editor-workflow-vue",
    addUiMessages: true,
    visible: uiType === "next",
  },
  {
    name: "JobEditOptionsApp",
    component: { OptionsEditorSection },
    elementClass: "job-editor-options-vue",
    addUiMessages: true,
    addEventBus: true,
    visible: uiType === "next",
  },
];

const mountSection = (section) => {
  section.elements = document.body.getElementsByClassName(section.elementClass);

  if (!section.elements || section.elements.length === 0) {
    return;
  }
  try {
    section.elements.forEach((element) => {
      const app = createApp({
        name: section.name,
        components: { ...section.component },
        data() {
          return section.addEventBus ? { EventBus } : {};
        },
      });
      app.use(uiv);
      app.use(i18n);
      if (section.addUiMessages) {
        app.provide("addUiMessages", async (messages) => {
          const newMessages = messages.reduce(
            (acc, message) => (message ? { ...acc, ...message } : acc),
            {},
          );
          const locale = window._rundeck.locale || "en_US";
          const lang = window._rundeck.language || "en";
          return updateLocaleMessages(i18n, locale, lang, newMessages);
        });
      }
      if (section.addCookies) {
        app.use(VueCookies);
      }
      app.use(pinia);
      app.mount(element);
    });
  } catch (e) {
    console.warn(e, section);
  }
};

jobSections.forEach((section) => section.visible && mountSection(section));

//on job edit page listen for dom content changes and install UI Sockets
window.addEventListener("DOMContentLoaded", (event) => {
  // Job Editing page - Workflow Tab

  const elem = document.querySelector("#workflowContent .pflowlist.edit");
  if (elem) {
    observer.observe(elem, { subtree: true, childList: true });
  }
});
