// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {markRaw} from 'vue'
import moment from 'moment'

import App from './App.vue'
import {getRundeckContext} from '../../../library'

let locale = window._rundeck.locale || 'en_US'
moment.locale(locale)

const rootStore = getRundeckContext().rootStore
rootStore.ui.addItems([
    {
        section: 'user-summary',
        location: 'main',
        visible: true,
        widget: markRaw(App),
    }
])