// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import {createApp} from 'vue'
import VueCookies from "vue-cookies";
import * as uiv from 'uiv'

import { getRundeckContext } from '../../../library'
import LogViewer from '../../../library/components/execution-log/logViewer.vue'
import './nodeView'
import {initI18n} from "../../utilities/i18n"

const VIEWER_CLASS = 'execution-log-viewer'

let MOUNTED = false

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
mainPanel.addEventListener('scroll', () => {
  scrollTop = mainPanel.scrollTop
})

window.onhashchange = () => {
  mainPanel.scrollTop = scrollTop
}

function mount(e) {
  const rootStore = getRundeckContext().rootStore
  // Create VueI18n instance with options
  const i18n = initI18n()

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
    executionId="${e.dataset.executionId}"
    :jumpToLine="${jumpToLine || null}"
    ref="viewer"
    :root-store="rootStore"
    ${e.dataset.trimOutput ? `trimOutput="${e.dataset.trimOutput}"` : ""}
  />
  `

  const vue = createApp({
    name:"LogViewerApp",
    components: {LogViewer},
    template: template,
    data() {
      return { rootStore }
    },
    provide: {
      rootStore
    }
  })
  vue.use(VueCookies)
  vue.use(uiv)
  vue.use(i18n)
  vue.mount(e)

  /** Puts line number in url HASH */
  window._rundeck.eventBus.on('line-select', (e) => {
    const hash = window.location.hash
    window.location.hash = `${hash.split('L')[0]}L${e}`
  })

  /** Removes line number from url hash */
  window._rundeck.eventBus.on('line-deselect', (e) => {
    const newHash = `${window.location.hash.split('L')[0]}`

    const panel = document.getElementById('section-main')
    const scrollPos = panel.scrollTop
    window.location.hash = newHash
    panel.scrollTop = scrollPos

  })
}
