// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import * as uiv from 'uiv'
import VueI18n from 'vue-i18n'
import VueCookies from 'vue-cookies'

import LogViewer from '@/components/execution-log/logViewer.vue'
import uivLang from '../../utilities/uivi18n'

const VIEWER_CLASS = 'execution-log-viewer'

let MOUNTED = false

let locale = window._rundeck.locale || 'en_US'
let lang = window._rundeck.language || 'en'

// include any i18n injected in the page by the app
let messages =
    {
      [locale]: Object.assign(
        {},
        uivLang[locale] || uivLang[lang] || {},
        window.Messages
      )
    }
Vue.config.productionTip = false

Vue.use(VueI18n)
Vue.use(VueCookies)
Vue.use(uiv)

const els = document.body.getElementsByClassName(VIEWER_CLASS)

/**
 * Watches the parent element for style change and mounts
 * the Vue component if it becomes visible
 */
let observer = new MutationObserver(function(mutations) {
  mutations.forEach(function(mutationRecord) {
    const parent = mutationRecord.target
    if (parent.offsetParent !== null && !MOUNTED) {
      console.log(parent, parent.display)
      console.log(parent.firstElementChild)
      MOUNTED = true
      mount(parent.firstElementChild)
    }
  })
})

for (let e of els) {
  if (e.parentNode.offsetParent !== null) {
    mount(e)
  } else {
    observer.observe(e.parentNode, {attributes: true, attributeFilter: ['style']})
  }
}

function mount(e) {
  console.log(e.dataset)
  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: locale, // set locale
    messages // set locale messages,

  })
  /* eslint-disable no-new */
  new LogViewer({
    el: e,
    i18n,
    propsData: {
      executionId: e.dataset.executionId,
      follow: e.dataset.follow || false,
      jumpToLine: e.dataset.jumpToLine,
      theme: e.dataset.theme
    }
  })
}
