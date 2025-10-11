import { defineStore } from "pinia";
import { getJobDefinition, postJobDefinition } from "../services/jobEdit";
import { JobDefinition } from "../types/jobs/JobDefinition";
import { JobStore } from "../types/stores/JobsStoreTypes";
import { contextVariables, ContextVariablesByType } from "./contextVariables";

export const useJobStore = defineStore("jobs", {
  state: (): JobStore => {
    return {
      activeId: "",
      jobs: {} as any,
      contextVariables: contextVariables() as ContextVariablesByType,
    };
  },
  getters: {
    jobDefinition: (state): JobDefinition | undefined =>
      state.activeId ? state.jobs[state.activeId] : undefined,
  },
  actions: {
    updateJobDefinition(job: JobDefinition, jobId?: string) {
      if (!jobId) {
        jobId = job.id;
      }
      if (!jobId) {
        jobId = this.activeId;
      }
      const existingJobDefinition: JobDefinition = this.jobs[jobId];
      if (existingJobDefinition) {
        this.jobs[jobId] = {
          ...existingJobDefinition,
          ...job,
        };
      } else {
        this.jobs[jobId] = job;
      }
    },
    async setActiveId(activeId: string): Promise<void> {
      this.activeId = activeId || "!new";
      await this.updateJobDefinition({}, this.activeId);
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
    async saveJobDefinition(job: JobDefinition, options: any = {}) {
      try {
        const resp = await postJobDefinition([job], options);
        if (resp.succeeded && resp.succeeded.length === 1) {
          const savedJob = resp.succeeded[0];
          this.updateJobDefinition({
            ...job,
            id: savedJob.id!,
            uuid: savedJob.id!,
          });
        }
      } catch (e) {
        console.warn(e);
      }
    },
  },
});
