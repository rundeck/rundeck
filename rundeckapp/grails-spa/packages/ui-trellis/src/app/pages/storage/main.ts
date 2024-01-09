import {createApp} from 'vue'
import * as uiv from 'uiv'

import KeyStoragePage from "../../../library/components/storage/KeyStoragePage.vue";
import KeyStorageView from "../../../library/components/storage/KeyStorageView.vue";
import KeyStorageEdit from "../../../library/components/storage/KeyStorageEdit.vue";
import {initI18n, updateLocaleMessages} from "../../utilities/i18n"
import {UiMessage} from "../../../library/stores/UIStore";

const i18n = initI18n()

const elm = document.getElementById('keyStoragePage')

const vue = createApp({
    name: 'StorageApp',
    components: { KeyStoragePage, KeyStorageView, KeyStorageEdit },
})
vue.use(uiv)
vue.use(i18n)
vue.provide('addUiMessages', async (messages) => {
    const newMessages = messages.reduce((acc:any, message:UiMessage) => message ? ({...acc, ...message}) : acc, {})
    const locale = window._rundeck.locale || 'en_US'
    const lang = window._rundeck.language || 'en'
    return updateLocaleMessages(i18n, locale, lang, newMessages)
})
vue.mount(elm)

