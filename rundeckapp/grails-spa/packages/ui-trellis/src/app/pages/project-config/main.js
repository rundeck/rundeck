// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import VueCookies from 'vue-cookies'
import * as uiv from 'uiv'
import PageConfirm from '../../../library/components/utils/PageConfirm.vue'
import {getRundeckContext} from '../../../library'
import PluginSetConfig from './PluginSetConfig.vue'
import ProjectPluginGroups from "./ProjectPluginGroups.vue"
import {initI18n} from "../../utilities/i18n"


const context = getRundeckContext()
// Create VueI18n instance with options
const els = document.body.getElementsByClassName('project-config-plugins-vue')

for (let i = 0; i < els.length; i++) {
    const e = els[i]

    const i18n = initI18n()
    const app = createApp({
        data() {
            return {
                EventBus: context.eventBus
            }
        },
        components: {
            PageConfirm,
            PluginSetConfig,
            ProjectPluginGroups
        }
    })
    app.use(VueCookies)
    app.use(uiv)
    app.use(i18n)
    app.mount(e)
}
