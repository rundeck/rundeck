import Vue from 'vue'
import KeyStoragePage from "../../../library/components/storage/KeyStoragePage.vue";

import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import uivLang from '../../../library/utilities/uivi18n'
import international from './i18n'

Vue.use(uiv)
Vue.use(VueI18n)

let messages = international.messages
let locale = window['_rundeck'].locale || 'en_US'
let lang = window['_rundeck'].language || 'en'

// include any i18n injected in the page by the app
const consolidatedMessages = {
    [locale]: Object.assign({},
        uivLang[locale] || uivLang[lang] || {},
        window['Messages'],
        messages[locale] || messages[lang] || messages['en_US'] || {}
    )
}

const i18n = new VueI18n({
    silentTranslationWarn: false,
    locale: locale, // set locale
    messages: { ...consolidatedMessages } // set locale messages,

})

const elm = document.getElementById('keyStoragePage')

const vue = new Vue({
    el: elm as Element,
    components: { KeyStoragePage },
    i18n
})