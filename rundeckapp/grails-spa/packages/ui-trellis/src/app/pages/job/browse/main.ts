import {defineComponent, markRaw, provide, reactive} from 'vue'
import {getRundeckContext} from '../../../../library'
import {
    JobBrowserStore,
    JobBrowserStoreInjectionKey
} from "../../../../library/stores/JobBrowser";
import moment from 'moment'
import {
  JobListFilterStore,
  JobListFilterLocalStorage,
  JobListFilterStoreInjectionKey
} from '../../../../library/stores/JobListFilterStore'
import {JobPageStore, JobPageStoreInjectionKey} from '../../../../library/stores/JobPageStore'
import {loadJsonData} from '../../../utilities/loadJsonData'
import JobListPage from './JobListPage.vue'
import * as uiv from "uiv";
import JobsPage from './JobsPage.vue'
import JobsPageHeader from './JobsPageHeader.vue'
import NextUiToggle from './NextUiToggle.vue'
import BulkSelectCheckbox from "./tree/BulkSelectCheckbox.vue";
import JobActionsMenu from './tree/JobActionsMenu.vue'
import JobRunButton from './tree/JobRunButton.vue'
import JobScheduleInfo from './tree/JobScheduleInfo.vue';
import JobScmStatus from './tree/JobScmStatus.vue'

function init() {
  const rootStore = getRundeckContext().rootStore;
  const uiMeta = loadJsonData("pageUiMeta")
  const uiType = uiMeta?.uiType||'current';
  rootStore.ui.addItems([
    {
      section: "theme-select",
      location: "after",
      visible: true,
      widget: markRaw(NextUiToggle)
    }
  ])
  if(uiType!=='next'){
    return
  }
  const pageQueryParams = loadJsonData("pageQueryParams")

  moment.locale(getRundeckContext().locale||'en_US')
  const page = rootStore.jobPageStore
  const browse = page.getJobBrowser()
  const jobListFilterStoreObj = new JobListFilterStore(
    getRundeckContext().projectName,
    new JobListFilterLocalStorage()
  );
  const jobPageStore = reactive(page);
  const jobBrowserStore = reactive(browse)
  rootStore.ui.addItems([
      {
          section: "job-list-page",
          location: "main",
          visible: true,
          widget: markRaw(
              defineComponent({
                  name: "JobPageMain",
                  components: { JobsPage },
                  props: ["itemData"],
                  setup() {
                      provide(JobBrowserStoreInjectionKey, jobBrowserStore);
                      provide(JobPageStoreInjectionKey, jobPageStore);
                    return {
                      pageQueryParams
                    }
                  },
                  template: `
          <jobs-page :query-params="pageQueryParams?.queryParams" />`,
              })
          ),
      },
      {
        section: 'main-content',
        location: 'before',
        visible: true,
        widget: markRaw(defineComponent({
          name: 'JobsPageHeaderWrap',
          components: {JobsPageHeader},
          setup() {
            provide(JobBrowserStoreInjectionKey, jobBrowserStore);
            provide(JobPageStoreInjectionKey, jobPageStore);
            provide(JobListFilterStoreInjectionKey, jobListFilterStoreObj);
            return {
              pageQueryParams
            }
          },
          template: `<jobs-page-header :query-params="pageQueryParams?.queryParams"/>`
        }))
      },
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
          location: "before-job-name",
          order: 10,
          visible: true,
          widget: markRaw(JobScmStatus),
      },
      {
          section: "job-browse-item",
          location: "after-job-name",
          visible: true,
          order: 1000,
          widget: markRaw(JobActionsMenu),
      },
  ]);
}

window.addEventListener('DOMContentLoaded', init)