import { defineStore } from "pinia";
import { getJobDefinition, postJobDefinition } from "../services/jobEdit";
import { JobDefinition } from "../types/jobs/JobDefinition";
import { JobStore } from "../types/stores/JobsStoreTypes";

export const useJobStore = defineStore("jobs", {
  state: (): JobStore => ({ activeId: "", jobs: {} as any }),
  getters: {
  },
  actions: {
      try {
        const jobDefinition = await getJobDefinition(jobId);
        if (jobDefinition.length > 0) {
          this.activeId = jobId;
        }
      } catch (e) {
        console.warn(e);
      }
    },
      try {
        if (resp.succeeded && resp.succeeded.length === 1) {
          const savedJob = resp.succeeded[0];
            ...job,
        }
      } catch (e) {
        console.warn(e);
      }
    },
  },
});
