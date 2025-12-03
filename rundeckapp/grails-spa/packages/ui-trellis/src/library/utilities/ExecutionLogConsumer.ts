import {
  ExecutionOutputGetResponse,
  ExecutionStatusGetResponse,
  JobWorkflowGetResponse,
  ExecutionOutput,
  ExecutionOutputEntry,
} from "../types/rundeckApi";
import { api } from "../services/api";

import { RenderedStepList, JobWorkflow } from "./JobWorkflow";

type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>;

export interface IRenderedEntry extends ExecutionOutputEntry {
  renderedStep?: RenderedStepList;
  renderedContext?: string;
  lineNumber: number;
  stepType?: string;
}

export type EnrichedExecutionOutput = Omit<ExecutionOutput, "entries"> & {
  entries: IRenderedEntry[];
};

const BACKOFF_MIN = 100;
const BACKOFF_MAX = 5000;

export class ExecutionLog {
  offset = 0;
  size = 0;
  completed = false;
  execCompleted = false;
  backoff = 0;
  lineNumber = 0;

  private jobWorkflowProm!: Promise<JobWorkflow>;
  private executionStatusProm!: Promise<ExecutionStatusGetResponse>;

  constructor(
    readonly id: string,
  ) {}

  /** Optional method to populate information about execution output */
  async init() {
    // Replace client.executionOutputGet with api.get
    const response = await api.get(`execution/${this.id}/output`, {
      params: {
        offset: "0",
        maxlines: 1,
      },
    });

    const resp = response.data;
    this.execCompleted = resp.execCompleted;
    this.size = resp.totalSize;
  }

  async getJobWorkflow() {
    if (!this.jobWorkflowProm) {
      this.jobWorkflowProm = (async () => {
        const status = await this.getExecutionStatus();
        if (!status.job) {
          return new JobWorkflow([
            { exec: status.description, type: "exec", nodeStep: "true" },
          ]);
        }
        // Replace client.jobWorkflowGet with api.get
        const response = await api.get(`job/${status.job.id}/workflow`);
        return new JobWorkflow(response.data.workflow);
      })();
    }
    return this.jobWorkflowProm;
  }

  async getExecutionStatus() {
    if (!this.executionStatusProm) {
      // Replace client.executionStatusGet with api.get
      this.executionStatusProm = api
        .get(`execution/${this.id}`)
        .then((response) => response.data as ExecutionStatusGetResponse);
    }

    return this.executionStatusProm;
  }

  async getOutput(maxLines: number): Promise<ExecutionOutputGetResponse> {
    await this.waitBackOff();

    // Replace client.executionOutputGet with api.get
    const response = await api.get(`execution/${this.id}/output`, {
      params: {
        offset: this.offset.toString(),
        maxlines: maxLines,
      },
    });

    const res = response.data;
    this.offset = parseInt(res.offset);
    this.size = res.totalSize;
    this.completed = res.completed && res.execCompleted;

    if (!this.completed && res.entries.length == 0) {
      this.increaseBackOff();
    } else {
      this.decreaseBackOff();
    }

    return res;
  }

  async waitBackOff() {
    if (this.backoff == 0) {
      return void 0;
    } else {
      return new Promise<void>((res, rej) => {
        setTimeout(res, this.backoff);
      });
    }
  }

  private increaseBackOff() {
    // TODO: Jitter https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
    this.backoff = Math.min(
      Math.max(this.backoff, BACKOFF_MIN) * 2,
      BACKOFF_MAX,
    );
  }

  private decreaseBackOff() {
    if (this.backoff == 0) return;

    const backoff = this.backoff / 2;
    this.backoff = backoff < BACKOFF_MIN ? 0 : backoff;
  }

  async getEnrichedOutput(maxLines: number): Promise<EnrichedExecutionOutput> {
    const [workflow, res] = await Promise.all([
      this.getJobWorkflow(),
      this.getOutput(maxLines),
    ]);

    const enrichedEntries = res.entries.map((e) => {
      this.lineNumber++;
      return {
        lineNumber: this.lineNumber,
        renderedStep: e.stepctx
          ? workflow.renderStepsFromContextPath(e.stepctx!)
          : undefined,
        renderedContext: e.stepctx
          ? workflow.renderContextString(e.stepctx!)
          : undefined,
        stepType: e.stepctx ? workflow.contextType(e.stepctx!) : undefined,
        ...e,
      };
    });

    return {
      ...res,
      entries: enrichedEntries,
    };
  }
}
