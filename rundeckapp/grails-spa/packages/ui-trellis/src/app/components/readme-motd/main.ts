
import {defineComponent, markRaw} from 'vue'
import {getRundeckContext} from "../../../library";
import EditProjectFile from "./EditProjectFile.vue";


import messages from './i18n'

const _i18n = messages as any

// Create VueI18n instance with options
let locale = getRundeckContext().locale || 'en_US'
let lang = getRundeckContext().language || 'en'

// include any i18n injected in the page by the app
let i18nMessages =
    {
        [locale]: Object.assign(
            {},
            _i18n[locale] || _i18n[lang] || _i18n['en_US'] || {}
        )
    }

let rundeckContext = getRundeckContext()

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

            data() {
                return {
                    filename: '',
                    displayConfig: [],
                    project: '',
                    authAdmin: false,
                };
            },
            created() {
                this.addUiMessages([i18nMessages[locale]]);
                this.filename = this.itemData.filename;
                // code to handle displayConfig
                if (typeof this.itemData.displayConfig === 'string') {
                    this.displayConfig = this.itemData.displayConfig.replace(/^\[|\]$/g, '').split(', ')
                }

                this.project = this.itemData.project;
                this.authAdmin = Boolean(this.itemData.authAdmin);
            },
            template: `
              <edit-project-file :filename="filename" :display-config="displayConfig" :project="project"
                                 :auth-admin="authAdmin"/>\`
            `,
        }))
    }
])