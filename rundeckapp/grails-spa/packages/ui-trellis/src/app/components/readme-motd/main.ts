import { defineComponent, markRaw } from "vue";
import { getRundeckContext } from "../../../library";
import EditProjectFile from "./EditProjectFile.vue";

import messages from "./i18n";

const _i18n = messages as any;

// Create VueI18n instance with options
const locale = getRundeckContext().locale || "en_US";
const lang = getRundeckContext().language || "en";

// include any i18n injected in the page by the app
const i18nmessages = {
  [locale]: Object.assign(
    {},
    _i18n[locale] || _i18n[lang] || _i18n["en_US"] || {},
  ),
};

const rundeckContext = getRundeckContext();

rundeckContext.rootStore.ui.addItems([
  {
    section: "edit-project-file",
    location: "main",
    visible: true,
    widget: markRaw(
      defineComponent({
        components: {
          EditProjectFile,
        },
        inject: ["addUiMessages"],
        props: ["itemData"],
        created() {
          this.addUiMessages([i18nmessages[locale]]);
        },
        template: `
                <edit-project-file :filename="itemData.filename" :display-config="itemData.displayConfig" :project="itemData.project" :auth-admin="itemData.authAdmin"/>`,
      }),
    ),
  },
]);
