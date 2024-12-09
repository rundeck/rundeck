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
    updateJobDefinition(job: JobDefinition, jobId: string) {
      const existingJobDefinition: JobDefinition = this.jobs[jobId || job.id];
      if (existingJobDefinition) {
        this.jobs[jobId || job.id] = {
          ...existingJobDefinition,
          ...job,
        };
      } else {
        this.jobs[jobId || job.id] = job;
      }
    },
    async fetchJobDefinition(jobId: string, setJobAsActive: boolean = true) {
      try {
        const jobDefinition = await getJobDefinition(jobId);
        if (jobDefinition.length > 0) {
          this.updateJobDefinition(jobDefinition[0], jobId);
          if (setJobAsActive) {
            this.activeId = jobId;
          }
        }
      } catch (e) {
        console.warn(e);
      }
    },
    async saveJobDefinition(job: JobDefinition, options: any) {
      try {
        const resp = await postJobDefinition([job], options);
        if (resp.succeeded && resp.succeeded.length === 1) {
          const savedJob = resp.succeeded[0];
          this.updateJobDefinition({
            ...job,
            id: savedJob.id,
            uuid: savedJob.id,
          });
        }
      } catch (e) {
        console.warn(e);
      }
    },
  },
});
