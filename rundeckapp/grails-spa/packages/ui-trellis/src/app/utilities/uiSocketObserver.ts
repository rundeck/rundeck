

//on job edit page list for dom content changes

import * as uiv from 'uiv'
import {getRundeckContext} from '../../library'
import UiSocket from '../../library/components/utils/UiSocket.vue'
import {initI18n, updateLocaleMessages} from "./i18n"
import {createApp} from 'vue'

export const observer = new MutationObserver(function (mutations_list) {
    mutations_list.forEach(function (mutation) {
        mutation.addedNodes.forEach(function (added_node) {
            if (added_node.className && added_node.getElementsByClassName('vue-ui-socket')?.length > 0) {
                const i18n = initI18n()
                const rootStore = getRundeckContext().rootStore
                const eventBus = getRundeckContext().eventBus

                for (let socketElem of added_node.getElementsByClassName('vue-ui-socket')) {
                    const vue = createApp( {
                        name: 'DynamicUiSocketRoot',
                        components: { UiSocket },
                        provide: {
                            rootStore
                        },
                        props: ['eventBus'],
                        inject: ['addUiMessages'],
                    }, {
                        eventBus
                    })

                    vue.provide('addUiMessages', async (messages) => {
                        const newMessages = messages.reduce((acc, message) => message ? ({...acc, ...message}) : acc, {})
                        const locale = window._rundeck.locale || 'en_US'
                        const lang = window._rundeck.language || 'en'
                        return updateLocaleMessages(i18n, locale, lang, newMessages)
                    })

                    vue.use(i18n)
                    vue.use(uiv)
                    vue.mount(socketElem, true)
                }
            }
        })
    })
})