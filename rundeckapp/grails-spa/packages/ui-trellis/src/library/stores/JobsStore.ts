import { defineStore } from "pinia";
import { getJobDefinition, postJobDefinition } from "../services/jobEdit";
import { JobDefinition } from "../types/jobs/JobDefinition";
import { JobStore } from "../types/stores/JobsStoreTypes";

export const useJobStore = defineStore("jobs", {
  state: (): JobStore => ({ activeId: "", jobs: {} as any }),
  getters: {
    jobDefinition: (state): JobDefinition => state.jobs[state.activeId],
  },
  actions: {
    async fetchJobDefinition(jobId: string) {
      try {
        const jobDefinition = await getJobDefinition(jobId);
        if (jobDefinition.length > 0) {
          this.jobs[jobId] = jobDefinition[0];
          this.activeId = jobId;
        }
      } catch (e) {
        console.warn(e);
      }
    },
    async saveJobDefinition(job: JobDefinition[], options: any) {
      try {
        const resp = await postJobDefinition(job, options);
        if (resp.succeeded && resp.succeeded.length === 1) {
          const savedJob = resp.succeeded[0];
          this.jobs[savedJob.id] = {
            ...job,
            id: savedJob.id,
            uuid: savedJob.id,
          };
        }
      } catch (e) {
        console.warn(e);
      }
    },
  },
});
