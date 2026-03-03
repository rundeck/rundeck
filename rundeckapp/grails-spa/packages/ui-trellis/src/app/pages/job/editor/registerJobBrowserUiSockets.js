import { defineComponent, markRaw, provide, reactive } from "vue";
import { getRundeckContext } from "@/library";
import { loadJsonData } from "@/app/utilities/loadJsonData";
import { JobBrowserStoreInjectionKey } from "@/library/stores/JobBrowser";
import { JobPageStoreInjectionKey } from "@/library/stores/JobPageStore";
import JobListPage from "@/app/pages/job/browse/JobListPage.vue";
import BulkSelectCheckbox from "@/app/pages/job/browse/tree/BulkSelectCheckbox.vue";
import JobActionsMenu from "@/app/pages/job/browse/tree/JobActionsMenu.vue";
import JobRunButton from "@/app/pages/job/browse/tree/JobRunButton.vue";
import JobScheduleInfo from "@/app/pages/job/browse/tree/JobScheduleInfo.vue";
import JobScmStatus from "@/app/pages/job/browse/tree/JobScmStatus.vue";

/**
 * Registers job-list-page and job-browse-item ui-socket widgets for the job editor.
 * Required for JobRefFormFields "Choose a job" modal to work without loading job/browse.js.
 */
export function registerJobBrowserUiSockets() {
  const rootStore = getRundeckContext().rootStore;
  const page = rootStore.jobPageStore;
  const pageQueryParams = loadJsonData("pageQueryParams");
  if (pageQueryParams?.queryParams?.groupPath) {
    page.browsePath = pageQueryParams.queryParams.groupPath;
    page.query["groupPath"] = pageQueryParams.queryParams.groupPath;
  } else {
    page.browsePath = "";
    page.query["groupPath"] = "";
  }
  const browse = page.getJobBrowser();
  const jobPageStore = reactive(page);
  const jobBrowserStore = reactive(browse);
  const jobTreeMeta = loadJsonData("jobTreeUiMeta");
  const showActions = !jobTreeMeta?.hideActions;

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
          <JobListPage v-bind="itemData" />`,
        }),
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
      visible: showActions,
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
      visible: showActions,
      order: 1000,
      widget: markRaw(JobActionsMenu),
    },
  ]);
}
