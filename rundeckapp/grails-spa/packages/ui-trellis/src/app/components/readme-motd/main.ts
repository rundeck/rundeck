
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
        section: 'edit-project-file',
        location: 'main',
        visible: true,
        widget: markRaw(defineComponent({
            components: {
                EditProjectFile
            },
            props: ['itemData'],
            inject: ['addUiMessages'],
            //  configuring the code from here
            data() {
                return {
                    // Initializing the data to be used in the component
                    filename: '',
                    displayConfig: [],
                    project: '',
                    authAdmin: false,
                };
            },
            created() {
                this.addUiMessages([i18nMessages[locale]]);
                // Performing transformation on the data
                this.filename = this.itemData.filename;
                this.displayConfig = Array.isArray(this.itemData.displayConfig) ? this.itemData.displayConfig.filter((item: any) => typeof item === 'string') : [];

                this.project = this.itemData.project;
                this.authAdmin = Boolean(this.itemData.authAdmin);
            },
            template: `
              <edit-project-file :filename="filename" :display-config="displayConfig" :project="project" :auth-admin="authAdmin"/>`
            `
        }))
    }
])
