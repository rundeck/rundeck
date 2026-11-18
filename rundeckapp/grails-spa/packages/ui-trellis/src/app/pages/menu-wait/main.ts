import { createApp } from "vue";
import * as uiv from "uiv";

import MenuWaitPage from "./MenuWaitPage.vue";
import { getRundeckContext } from "../../../library";
import {
  initI18n,
  commonAddUiMessages,
  type LocalizedMessages,
} from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";
import { configurePrimeVue } from "../../../library/utilities/primeVueConfig";
import type { ExportWaitData } from "../../components/menu-wait/types/exportWaitTypes";

const i18n = initI18n();

const elm = document.getElementById("menuWaitPage");

const rundeckContext = getRundeckContext();
const data: ExportWaitData = rundeckContext.data?.exportWaitData || {
  token: "",
  project: "",
  ready: false,
  percentage: 0,
  instance: null,
  iproject: null,
  pollUrl: "",
  downloadUrl: "",
  refreshUrl: "",
  returnUrl: "",
};

const vue = createApp({
  name: "MenuWaitApp",
  components: { MenuWaitPage },
  data() {
    return { wait: data };
  },
  template: `<menu-wait-page
    :token="wait.token"
    :project="wait.project"
    :initial-ready="!!wait.ready"
    :initial-percentage="wait.percentage || 0"
    :instance="wait.instance"
    :iproject="wait.iproject"
    :poll-url="wait.pollUrl"
    :download-url="wait.downloadUrl"
    :refresh-url="wait.refreshUrl"
    :return-url="wait.returnUrl"
  />`,
});
vue.use(uiv);
vue.use(i18n);
configurePrimeVue(vue);
vue.provide(
  "addUiMessages",
  async (messages: UiMessage[] | LocalizedMessages) =>
    commonAddUiMessages(i18n, messages),
);
if (elm) {
  vue.mount(elm);
}
