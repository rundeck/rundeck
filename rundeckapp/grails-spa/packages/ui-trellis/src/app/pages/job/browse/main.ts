import {defineComponent, markRaw, provide, reactive} from 'vue'
import {getRundeckContext} from '../../../../library'
import {
    JobBrowserStore,
    JobBrowserStoreInjectionKey,
    JobPageStore,
    JobPageStoreInjectionKey,
} from "../../../../library/stores/JobBrowser";
import JobListPage from './JobListPage.vue'
import * as uiv from "uiv";
import BulkSelectCheckbox from "./tree/BulkSelectCheckbox.vue";
import JobActionsMenu from './tree/JobActionsMenu.vue'
import JobRunButton from './tree/JobRunButton.vue'
import JobScheduleInfo from './tree/JobScheduleInfo.vue';

function init() {
  const rootStore = getRundeckContext().rootStore;
  const page = new JobPageStore();
  const browse = new JobBrowserStore(getRundeckContext().projectName, "");

  const jobPageStore = reactive(page);
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
                      provide(JobPageStoreInjectionKey, jobPageStore);
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
          order: 0,
          visible: true,
          widget: markRaw(BulkSelectCheckbox),
      },
      {
          section: "job-browse-item",
          location: "before-job-name",
          order: 1,
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