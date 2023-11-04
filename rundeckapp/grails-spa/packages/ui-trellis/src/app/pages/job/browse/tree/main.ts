import {defineComponent, markRaw, provide, reactive} from 'vue'
import {getRundeckContext} from '../../../../../library'
import {JobBrowserStore, JobBrowserStoreInjectionKey} from '../../../../../library/stores/JobBrowser'
import Browser from './Browser.vue'
import * as uiv from 'uiv'

function init() {
  const rootStore = getRundeckContext().rootStore
  const browse = new JobBrowserStore(getRundeckContext().projectName, '')

  const jobBrowserStore = reactive(browse)
  rootStore.ui.addItems([
    {
      section: 'job-group-browser',
      location: 'main',
      visible: true,
      widget: markRaw(defineComponent({
        name: 'JobGroupBrowser',
        components: {Browser},
        props: ['itemData'],
        setup() {
          provide(JobBrowserStoreInjectionKey, jobBrowserStore)
        },
        template: `
          <Browser :path="itemData?itemData.path:''"/>`,
      })),
    }
  ])
}

window.addEventListener('DOMContentLoaded', init)