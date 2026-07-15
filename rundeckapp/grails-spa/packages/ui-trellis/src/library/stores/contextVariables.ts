import { RundeckContext as RundeckContextType } from "../interfaces/rundeckWindow";
import { getRundeckContext } from "../rundeckService";
import { JobOption } from "../types/jobs/JobEdit";

export type ContextVariable = {
  name: string;
  title: string;
  description?: string;
  type: "job" | "node" | "result" | "execution" | "option" | "global";
};

export type ContextVariablesByType = {
  [key: string]: ContextVariable[];
};

export const createOptionVariables = (
  options: JobOption[],
): ContextVariable[] => {
  return options.map((variable: JobOption): ContextVariable => {
    return {
      name: variable.name,
      title: variable.label || "",
      ...(variable.description ? { description: variable.description } : {}),
      type: "option",
    };
  });
};

export const contextVariables = (): ContextVariablesByType => ({
  job: [
    {
      name: "id",
      title: "Job ID",
      type: "job",
    },
    {
      name: "execid",
      title: "Execution ID",
      type: "job",
    },
    {
      name: "executionType",
      title: "Execution Type",
      type: "job",
    },
    {
      name: "name",
      title: "Job Name",
      type: "job",
    },
    {
      name: "group",
      title: "Job Group",
      type: "job",
    },
    {
      name: "username",
      title: "Name of user executing the job",
      type: "job",
    },
    {
      name: "project",
      title: "Project name",
      type: "job",
    },
    {
      name: "loglevel",
      title: "Execution log level",
      description: "Logging level, one of: INFO, DEBUG",
      type: "job",
    },
    {
      name: "user.email",
      title: "Email of user executing the job",
      type: "job",
    },
    {
      name: "retryAttempt",
      title: "Retry attempt number",
      type: "job",
    },
    {
      name: "retryInitialExecId",
      title: "Retry Original Execution ID",
      type: "job",
    },
    {
      name: "wasRetry",
      title: "True if execution is a retry",
      type: "job",
    },
    {
      name: "threadcount",
      title: "Job Threadcount",
      type: "job",
    },
    {
      name: "filter",
      title: "Job Node Filter Query",
      type: "job",
    },
  ],
  execution: [
    {
      name: "id",
      title: "Execution ID",
      type: "execution",
    },
    {
      name: "href",
      title: "URL to the execution output view",
      type: "execution",
    },
    {
      name: "status",
      title: "Execution state (‘running’,‘failed’,‘aborted’,‘succeeded’)",
      type: "execution",
    },
    {
      name: "user",
      title: "User who started the job",
      type: "execution",
    },
    {
      name: "dateStarted",
      title: "Execution Start time ",
      type: "execution",
    },
    {
      name: "description",
      title: "Summary string for the execution",
      type: "execution",
    },
    {
      name: "argstring",
      title: "Argument string for any job options",
      type: "execution",
    },
    {
      name: "project",
      title: "Project name",
      type: "execution",
    },
    {
      name: "loglevel",
      title: "Execution log level",
      description: "Logging level, one of: INFO, DEBUG",
      type: "execution",
    },
    {
      name: "failedNodeListString",
      title: "Comma-separated list of any nodes that failed, if present",
      type: "execution",
    },
    {
      name: "succeededNodeListString",
      title: "Comma-separated list of any nodes that succeeded, if present",
      type: "execution",
    },
    {
      name: "abortedby",
      title: "User who aborted the execution",
      type: "execution",
    },
  ],
  node: [
    {
      name: "name",
      title: "Node Name",
      type: "node",
    },
    {
      name: "hostname",
      title: "Node Hostname",
      type: "node",
    },
    {
      name: "username",
      title: "Node username",
      type: "node",
    },
    {
      name: "description",
      title: "Node description",
      type: "node",
    },
    {
      name: "tags",
      title: "Node tags",
      type: "node",
    },
    {
      name: "os-name",
      title: "OS Name",
      type: "node",
    },
    {
      name: "os-family",
      title: "OS Family",
      type: "node",
    },
    {
      name: "os-arch",
      title: "OS Architecture",
      type: "node",
    },
    {
      name: "os-version",
      title: "OS Version",
      type: "node",
    },
  ],
  error_handler: [
    {
      name: "message",
      title: "Error Message",
      type: "result",
    },
    {
      name: "resultCode",
      title: "Result Code",
      description: "Exit code from an execution (if available)",
      type: "result",
    },
    {
      name: "failedNodes",
      title: "Failed Nodes List",
      type: "result",
    },
    {
      name: "reason",
      title: "Error Reason",
      description: "A code indicating the reason the step failed",
      type: "result",
    },
  ],
});
