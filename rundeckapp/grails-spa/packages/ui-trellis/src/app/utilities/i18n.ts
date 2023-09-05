import {nextTick} from "vue";
import {UiMessage} from '../../library/stores/UIStore'
import en_US from './locales/en_US'
import es_419 from './locales/es_419'
import fr_FR from './locales/fr_FR'
import ja_JP from './locales/ja_JP'
import pt_BR from './locales/pt_BR'
import zh_CN from './locales/zh_CN'
import uivLang from "../../library/utilities/uivi18n";
import {createI18n} from "vue-i18n";
import {mergeDeep} from "./objectUtils";

const internationalization = {
    en_US: en_US,
    es_419: es_419,
    fr_FR: fr_FR,
    ja_JP: ja_JP,
    pt_BR: pt_BR,
    zh_CN: zh_CN,
}

const initI18n = (options = {}) => {
    const locale = window._rundeck.locale || 'en_US'
    const lang = window._rundeck.language || 'en'
    const messages = {
        [locale]: Object.assign(
            {},
            uivLang[locale] || uivLang[lang] || {},
            internationalization[locale] || internationalization[lang] || internationalization['en_US'] || {}
        )
    }

    // Create VueI18n instance with options
    return createI18n({
        silentTranslationWarn: true,
        locale: locale, // set locale
        messages, // set locale messages
        ...options,
    })
}

const updateLocaleMessages = async (i18n, locale, lang, messages) => {
    const baseMessages = {
        ...(uivLang[locale] || uivLang[lang] || {}),
        ...(internationalization[locale] || internationalization[lang] || internationalization['en_US'] || {})
    }
    i18n.global.setLocaleMessage(locale, mergeDeep(baseMessages, messages))
    return nextTick()
}
/**
 * update locale messages for the i18n instance
 * @param i18n vue-i18n instance
 * @param messages new messages data
 */
const commonAddUiMessages = async (i18n, messages) => {
    const newMessages = messages.reduce((acc: any, message: UiMessage) => message ? ({...acc, ...message}) : acc, {})
    const locale = window._rundeck.locale || 'en_US'
    const lang = window._rundeck.language || 'en'
    return updateLocaleMessages(i18n, locale, lang, newMessages)
}

export {
    initI18n,
    updateLocaleMessages,
    commonAddUiMessages
}