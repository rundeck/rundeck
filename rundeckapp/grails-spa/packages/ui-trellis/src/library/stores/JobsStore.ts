import { defineStore } from "pinia";
import { getJobDefinition, postJobDefinition } from "../services/jobEdit";
import { JobDefinition, LogLevel } from "../types/jobs/JobDefinition";
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
    hasJob:
      (state) =>
      (jobId: string): boolean => {
        return !!state.jobs[jobId];
      },
  },
  actions: {
    updateJobDefinition(job: JobDefinition, jobId: string) {
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
    initializeJobPlaceholder() {
      this.updateJobDefinition(
        {
          id: "!new",
          loglevel: LogLevel.Info,
          nodeFilterEditable: false,
          nodesSelectedByDefault: true,
          name: "",
          scheduleEnabled: true,
          executionEnabled: true,
        },
        "!new",
      );
    },
    setActiveId(activeId: string): void {
      this.activeId = activeId || "!new";
    },
    async fetchJobDefinition(jobId: string, setJobAsActive: boolean = true) {
      try {
        const jobDefinition = await getJobDefinition(jobId);
        if (jobDefinition.length > 0) {
          this.updateJobDefinition(jobDefinition[0], jobId);
          if (setJobAsActive) {
            this.setActiveId(jobId);
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
          this.updateJobDefinition(
            {
              ...job,
              id: savedJob.id,
              uuid: savedJob.id,
            },
            savedJob.id,
          );

          if (this.activeId === "!new") {
            this.initializeJobPlaceholder();
          }
        }
      } catch (e) {
        console.warn(e);
      }
    },
  },
});
