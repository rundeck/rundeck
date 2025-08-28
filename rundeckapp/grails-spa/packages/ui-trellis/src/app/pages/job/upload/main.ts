import { defineComponent, markRaw, provide, reactive } from "vue";
import { getRundeckContext } from "../../../../library";
import BulkSelectCheckbox from "../browse/tree/BulkSelectCheckbox.vue";
import JobActionsMenu from "../browse/tree/JobActionsMenu.vue";
import JobRunButton from "../browse/tree/JobRunButton.vue";
import JobScheduleInfo from "../browse/tree/JobScheduleInfo.vue";
import JobScmStatus from "../browse/tree/JobScmStatus.vue";
import JobUploadPage from "./JobUploadPage.vue";
import { JobPageStoreInjectionKey } from "../../../../library/stores/JobPageStore";

function init() {
  const rootStore = getRundeckContext().rootStore;
  const page = rootStore.jobPageStore;
  const jobPageStore = reactive(page);
  rootStore.ui.addItems([
    {
      section: "job-upload-page",
      location: "main",
      visible: true,
      widget: markRaw(
        defineComponent({
          name: "JobUploadPageMain",
          components: { JobUploadPage },
          props: ["itemData"],
          setup() {
            provide(JobPageStoreInjectionKey, jobPageStore);
          },
          template: `
          <job-upload-page />`,
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
    // {
    //   section: "job-browse-item",
    //   location: "before-job-name",
    //   order: 1,
    //   visible: true,
    //   widget: markRaw(JobRunButton),
    // },
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

window.addEventListener("DOMContentLoaded", init);
