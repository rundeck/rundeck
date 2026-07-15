import { markRaw } from "vue";
import { getRundeckContext } from "../../../../library";
import JobStatsBasic from "./JobStatsBasic.vue";

const rootStore = getRundeckContext().rootStore;
export default function loadJobStats() {
  rootStore.ui.addItems([
    {
      section: "job-stats",
      location: "main",
      visible: true,
      widget: markRaw(JobStatsBasic),
    },
  ]);
}
