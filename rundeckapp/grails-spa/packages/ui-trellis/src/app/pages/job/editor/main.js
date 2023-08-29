// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import VueCookies from 'vue-cookies'
import * as uiv from 'uiv'
import moment from 'moment'

import NotificationsEditorSection from './NotificationsEditorSection.vue'
import ResourcesEditorSection from './ResourcesEditorSection.vue'
import SchedulesEditorSection from './SchedulesEditorSection.vue'
import OtherEditorSection from './OtherEditorSection.vue'
import {
    EventBus
} from '../../../../library/utilities/vueEventBus'
import {initI18n, updateLocaleMessages} from "../../../utilities/i18n"

let locale = window._rundeck.locale || 'en_US'
moment.locale(locale)

const i18n = initI18n()
const els = document.body.getElementsByClassName('job-editor-notifications-vue')

for (let i = 0; i < els.length; i++) {
    const e = els[i]
    const app = createApp({
        name:"JobEditNotificationsApp",
        data(){return{EventBus,}},
        components: { NotificationsEditorSection }
    })
    app.use(uiv)
    app.use(i18n)
    app.use(VueCookies)
    app.mount(e)
}
const resels = document.body.getElementsByClassName('job-editor-resources-vue')

for (let i = 0; i < resels.length; i++) {
    const e = resels[i]
    const rapp = createApp({
        name:"JobEditResourcesApp",
        data(){return{EventBus,}},
        components: { ResourcesEditorSection }
    })
    rapp.use(uiv)
    rapp.use(i18n)
    rapp.provide('addUiMessages', async (messages) => {
        const newMessages = messages.reduce((acc, message) => message ? ({...acc, ...message}) : acc, {})
        const locale = window._rundeck.locale || 'en_US'
        const lang = window._rundeck.language || 'en'
        return updateLocaleMessages(i18n, locale, lang, newMessages)
    })
    rapp.mount(e)
}
const scsels = document.body.getElementsByClassName('job-editor-schedules-vue')

for (let i = 0; i < scsels.length; i++) {
    const e = scsels[i]
    const sapp = createApp({
        name: "JobEditSchedulesApp",
        data() {
            return {EventBus,}
        },
        components: {SchedulesEditorSection}
    })
    sapp.use(uiv)
    sapp.use(i18n)
    sapp.provide('addUiMessages', async (messages) => {
        const newMessages = messages.reduce((acc, message) => message ? ({...acc, ...message}) : acc, {})
        const locale = window._rundeck.locale || 'en_US'
        const lang = window._rundeck.language || 'en'
        return updateLocaleMessages(i18n, locale, lang, newMessages)
    })
    sapp.mount(e)

    const othels = document.body.getElementsByClassName('job-editor-other-vue')

    for (let i = 0; i < othels.length; i++) {
        const e = othels[i]
        const oapp = createApp({
            name: "JobEditOtherApp",
            data() {
                return {EventBus,}
            },
            components: {OtherEditorSection}
        })
        oapp.use(uiv)
        oapp.use(i18n)
        oapp.mount(e)
    }
}
