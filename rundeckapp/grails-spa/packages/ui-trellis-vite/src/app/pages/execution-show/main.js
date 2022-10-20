// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import VueI18n from 'vue-i18n'

import { getRundeckContext } from '@/library/rundeckService'
import LogViewer from '@/library/components/execution-log/logViewer.vue'
import uivLang from '@/library/utilities/uivi18n'

import './nodeView'

const VIEWER_CLASS = 'execution-log-viewer'

const rootStore = getRundeckContext().rootStore

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

// Vue.use(VueI18n)
// Vue.use(VueCookies)
// Vue.use(uiv)

const els = document.body.getElementsByClassName(VIEWER_CLASS)

/**
 * Watches the parent element for style change and mounts
 * the Vue component if it becomes visible
 */
let observer = new MutationObserver(function(mutations) {
  mutations.forEach(function(mutationRecord) {
    const parent = mutationRecord.target
    if (parent.offsetParent !== null && !MOUNTED) {
      MOUNTED = true
      mount(parent.firstElementChild)
    }
  })
})

setTimeout(() => {
  for (let e of els) {
    if (window.getComputedStyle(e.parentElement).display !== 'none') {
      mount(e)
    } else {
      observer.observe(e.parentNode, {attributes: true, attributeFilter: ['style']})
    }
  }
}, 0)

/** Stop page from jumping to log anchor */
const mainPanel = document.getElementById('section-main')
if (location.hash) {
  setTimeout(function() {
    mainPanel.scrollTo(0, 0)
  }, 1)
}

let scrollTop = 0
mainPanel?.addEventListener('scroll', () => {
  scrollTop = mainPanel?.scrollTop
})

window.onhashchange = () => {
  mainPanel.scrollTop = scrollTop
}

function mount(e) {
  // Create VueI18n instance with options
  const i18n = new VueI18n({
    silentTranslationWarn: true,
    locale: locale, // set locale
    messages // set locale messages,

  })
  /* eslint-disable no-new */

  let jumpToLine
  const line = window.location.hash.split('L')[1]
  if (line)
    jumpToLine = parseInt(line)

  /**
   * Ant accesses the root Vue instance constructor.
   * Since the viewer is a class component that would make its
   * constructor the root constructor and chaos ensues...
   * */
  const template = `\
  <LogViewer
    v-if="this.$el.parentNode.display != 'none'"
    executionId="${e.dataset.executionId}"
    jumpToLine="${jumpToLine || null}"
    ref="viewer"
    trimOutput="${e.dataset.trimOutput}"
  />
  `

  const vue = new Vue({
    el: e,
    i18n,
    components: {LogViewer},
    template: template,
    mounted() {
      this.$refs.viewer.$on('line-select', (e) => this.$emit('line-select', e))
      this.$refs.viewer.$on('line-deselect', e => this.$emit('line-deselect', e))
    },
    provide: {
      rootStore
    }
  })

  /** Puts line number in url HASH */
  vue.$on('line-select', (e) => {
    const hash = window.location.hash
    window.location.hash = `${hash.split('L')[0]}L${e}`
  })

  /** Removes line number from url hash */
  vue.$on('line-deselect', (e) => {
    const newHash = `${window.location.hash.split('L')[0]}`

    const panel = document.getElementById('section-main')
    const scrollPos = panel.scrollTop
    window.location.hash = newHash
    panel.scrollTop = scrollPos

  })
}
