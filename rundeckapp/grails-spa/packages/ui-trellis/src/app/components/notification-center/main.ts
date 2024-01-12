import { defineComponent, markRaw } from 'vue'
import {getRundeckContext} from "../../../library";
import ProjectNotificationCenter from "./NotificationCenter.vue";

import messages from './i18n'

const _i18n = messages as any

// Create VueI18n instance with options
let locale = getRundeckContext().locale || 'en_US'
let lang = getRundeckContext().language || 'en'

// include any i18n injected in the page by the app
let i18nmessages =
    {
        [locale]: Object.assign(
            {},
            _i18n[locale] || _i18n[lang] || _i18n['en_US'] || {}
        )
    }

let rundeckContext = getRundeckContext()

rundeckContext.rootStore.ui.addItems([
    {
        section: 'project-notification-center',
        location: 'main',
        visible: true,
        widget: markRaw(defineComponent({
            components: {
                ProjectNotificationCenter
            },
            props: [],
            inject: ['addUiMessages'],
            created(){
                this.addUiMessages([i18nmessages[locale]])
            },
            template: `
                <project-notification-center />`
        }))
    }
])