import { createApp } from "vue";
import * as uiv from "uiv";

import KeyStoragePage from "../../../library/components/storage/KeyStoragePage.vue";
import KeyStorageView from "../../../library/components/storage/KeyStorageView.vue";
import KeyStorageEdit from "../../../library/components/storage/KeyStorageEdit.vue";
import {
  initI18n,
  commonAddUiMessages,
  type LocalizedMessages,
} from "../../utilities/i18n";
import { UiMessage } from "../../../library/stores/UIStore";
import { configurePrimeVue } from "../../../library/utilities/primeVueConfig";

const i18n = initI18n();

const elm = document.getElementById("keyStoragePage");

const vue = createApp({
  name: "StorageApp",
  components: { KeyStoragePage, KeyStorageView, KeyStorageEdit },
});
vue.use(uiv);
vue.use(i18n);
// Plugin config forms rendered here (e.g. storage plugins) use PrimeVue inputs for numeric properties
configurePrimeVue(vue);
vue.provide(
  "addUiMessages",
  async (messages: UiMessage[] | LocalizedMessages) =>
    commonAddUiMessages(i18n, messages),
);
if (elm) {
  vue.mount(elm);
}
