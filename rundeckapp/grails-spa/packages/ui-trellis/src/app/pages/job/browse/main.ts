import {defineComponent, markRaw, provide, reactive} from 'vue'
import {getRundeckContext} from '../../../../library'
import {JobBrowserStore, JobBrowserStoreInjectionKey} from '../../../../library/stores/JobBrowser'
import JobListPage from './JobListPage.vue'
import * as uiv from 'uiv'
import JobActionsMenu from './tree/JobActionsMenu.vue'
import JobRunButton from './tree/JobRunButton.vue'
import JobScheduleInfo from './tree/JobScheduleInfo.vue';

function init() {
  const rootStore = getRundeckContext().rootStore
  const browse = new JobBrowserStore(getRundeckContext().projectName, '')

  const jobBrowserStore = reactive(browse)
  rootStore.ui.addItems([
      {
          section: "job-list-page",
          location: "tree-browser",
          visible: true,
          widget: markRaw(
              defineComponent({
                  name: "JobGroupBrowser",
                  components: { JobListPage },
                  props: ["itemData"],
                  setup() {
                      provide(JobBrowserStoreInjectionKey, jobBrowserStore);
                  },
                  template: `
          <JobListPage />`,
              })
          ),
      },
      {
          section: "job-browse-item",
          location: "after-job-name:meta:schedule",
          visible: true,
          widget: markRaw(JobScheduleInfo),
      },
      {
          section: "job-browse-item",
          location: "before-job-name",
          visible: true,
          widget: markRaw(JobRunButton),
      },
      {
          section: "job-browse-item",
          location: "after-job-name",
          visible: true,
          widget: markRaw(JobActionsMenu),
      },
  ]);
}

window.addEventListener('DOMContentLoaded', init)