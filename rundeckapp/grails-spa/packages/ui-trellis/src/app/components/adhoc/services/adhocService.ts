import { api } from "../../../../library/services/api";
import { getRundeckContext } from "../../../../library/rundeckService";
import axios from "axios";

export interface RunAdhocRequest {
  project: string;
  exec: string;
  filter: string;
  filterExclude?: string;
  doNodedispatch?: string;
  nodeThreadcount?: number;
  nodeKeepgoing?: boolean;
}

export interface RunAdhocResponse {
  success?: boolean;
  id?: string | number;
  error?: string;
}

/**
 * Execute an adhoc command via REST API endpoint
 * Uses: POST /api/{api_version}/project/{project}/run/command/inline
 */
export async function runAdhocCommand(
  request: RunAdhocRequest,
): Promise<RunAdhocResponse> {
  const rundeckContext = getRundeckContext();
  
  // Build API endpoint URL: /api/{api_version}/project/{project}/run/command/inline
  // The api client already has baseURL set to rdBase + "api/" + apiVersion + "/"
  // So we use a relative path starting with "project/"
  const url = `project/${request.project}/run/command/inline`;

  // Request body must include project parameter
  const requestBody: RunAdhocRequest = {
    project: request.project,
    exec: request.exec,
    filter: request.filter,
    filterExclude: request.filterExclude,
    doNodedispatch: request.doNodedispatch,
    nodeThreadcount: request.nodeThreadcount,
    nodeKeepgoing: request.nodeKeepgoing,
  };

  const response = await api.post<RunAdhocResponse>(url, requestBody);

  if (response.data.error) {
    throw new Error(response.data.error);
  }

  if (!response.data.id) {
    throw new Error("No execution ID returned from server");
  }

  return response.data;
}

/**
 * Load execution follow fragment (HTML output)
 * This still uses the ajax endpoint as it returns HTML, not JSON
 * Note: This is not an API endpoint, so we use axios directly with full URL
 */
export async function loadExecutionFollow(
  executionId: string | number,
  mode: string = "tail",
): Promise<string> {
  // Use the ajax endpoint for HTML output (not converting this to API endpoint)
  const rundeckContext = getRundeckContext();
  const url = `${rundeckContext.rdBase}execution/followFragment?id=${executionId}&mode=${mode}`;

  // Use axios directly since this is not an API endpoint
  const response = await axios.get<string>(url, {
    headers: {
      "x-rundeck-ajax": "true",
      "Accept": "text/html",
    },
    responseType: "text",
  });

  if (response.status !== 200) {
    throw new Error(`Failed to load execution output: ${response.statusText}`);
  }

  return response.data;
}

export interface AbortExecutionResponse {
  abort?: {
    status: string;
    reason?: string;
  };
  execution?: {
    id: string | number;
    status: string;
    href: string;
  };
  error?: string;
}

export interface ExecutionDetails {
  id: number;
  status: string;
  project: string;
  failedNodes?: string[];
  successfulNodes?: string[];
  [key: string]: any;
}

/**
 * Abort/kill a running execution via REST API endpoint
 * Uses: POST /api/{api_version}/execution/{id}/abort
 */
export async function killExecution(
  executionId: string | number,
  forceIncomplete: boolean = false,
): Promise<AbortExecutionResponse> {
  // Build API endpoint URL: /api/{api_version}/execution/{id}/abort
  // The api client already has baseURL set to rdBase + "api/" + apiVersion + "/"
  // So we use a relative path starting with "execution/"
  const url = `execution/${executionId}/abort${forceIncomplete ? "?forceIncomplete=true" : ""}`;

  const response = await api.post<AbortExecutionResponse>(url);

  if (response.data.error) {
    throw new Error(response.data.error);
  }

  return response.data;
}

/**
 * Fetch execution details from API to get failedNodes and other execution information
 * Uses: GET /api/{api_version}/execution/{id}
 */
export async function getExecutionDetails(
  executionId: string | number,
): Promise<ExecutionDetails | null> {
  // Use execution API endpoint: /api/{api_version}/execution/{id}
  const executionApiUrl = `execution/${executionId}`;

  const response = await api.get(executionApiUrl);

  // The API returns { executions: [...] } when multiple, or flattened execution object when single:true
  let execution: any = null;
  if (response.data && Array.isArray(response.data.executions) && response.data.executions.length > 0) {
    // Multiple executions format
    execution = response.data.executions[0];
  } else if (response.data && response.data.id) {
    // Single execution format (when single:true, execution is flattened directly)
    execution = response.data;
  }

  if (!execution) {
    return null;
  }

  // Extract failedNodes from execution data (API returns as array from comma-separated string)
  const failedNodes: string[] = [];
  if (execution.failedNodes && Array.isArray(execution.failedNodes)) {
    failedNodes.push(...execution.failedNodes.filter((n: string) => n && n.trim().length > 0));
  } else if (execution.failedNodes && typeof execution.failedNodes === "string") {
    // Handle comma-separated string (fallback)
    failedNodes.push(
      ...execution.failedNodes
        .split(",")
        .map((n: string) => n.trim())
        .filter((n: string) => n.length > 0),
    );
  }

  return {
    ...execution,
    failedNodes: failedNodes.length > 0 ? failedNodes : undefined,
  };
}

