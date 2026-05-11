import { api } from "../../../../library/services/api";
import { getRundeckContext } from "../../../../library/rundeckService";
import { getAppLinks } from "../../../../library";
import { _genUrl } from "../../../../library/utilities/genUrl";
import axios from "axios";

export interface RunAdhocRequest {
  project: string;
  exec: string;
  filter: string;
  filterExclude?: string;
  doNodedispatch?: string;
  nodeThreadcount?: number;
  nodeKeepgoing?: boolean;
  // Meta fields can be either:
  // 1. Nested in meta object: { meta: { jobRunnerFilter: "...", ... } }
  // 2. Flat fields: { "meta.jobRunnerFilter": "...", "meta.jobRunnerFilterType": "...", ... }
  // The service will convert nested meta to flat query parameters
  meta?: {
    jobRunnerFilter?: string;
    jobRunnerFilterType?: string;
    jobRunnerFilterMode?: string;
    [key: string]: string | undefined;
  };
  // Allow any additional fields (including flat meta.* fields from form)
  [key: string]: any;
}

export interface RunAdhocResponse {
  success?: boolean;
  id?: string | number;
  error?: string;
}

/**
 * Execute an adhoc command via REST API endpoint
 * Uses: POST /api/{api_version}/project/{project}/run/command/inline
 * 
 * IMPORTANT: Sends data as query parameters (not JSON body) to match original jQuery.serialize() behavior
 * This ensures meta.* fields are sent as flat query parameters (meta.jobRunnerFilter, etc.)
 * which Grails automatically parses into the meta map object
 */
export async function runAdhocCommand(
  request: RunAdhocRequest,
): Promise<RunAdhocResponse> {
  const rundeckContext = getRundeckContext();
  
  // Build API endpoint URL: /api/{api_version}/project/{project}/run/command/inline/api
  // Use the new API authentication endpoint for v56+
  // The api client already has baseURL set to rdBase + "api/" + apiVersion + "/"
  // So we use a relative path starting with "project/"
  const url = `project/${request.project}/run/command/inline/api`;

  // Convert request to query parameters (matching original jQuery.serialize() behavior)
  // This ensures meta.* fields are sent as flat query parameters that Grails parses correctly
  const params: Record<string, any> = {
    formInput: "true", // Required by endpoint
    project: request.project,
    exec: request.exec,
    filter: request.filter,
    doNodedispatch: request.doNodedispatch || "true",
  };
  
  if (request.filterExclude) {
    params.filterExclude = request.filterExclude;
  }
  if (request.nodeThreadcount !== undefined) {
    params.nodeThreadcount = request.nodeThreadcount;
  }
  if (request.nodeKeepgoing !== undefined) {
    params.nodeKeepgoing = request.nodeKeepgoing;
  }
  
  // Add meta fields as flat query parameters (meta.jobRunnerFilter, etc.)
  // Grails will automatically parse these into the meta map object
  // IMPORTANT: For runner-related meta fields, if they're empty, set them to LOCAL_RUNNER defaults
  // Empty runner fields cause executions to hang waiting for a runner that doesn't exist
  // When no runner is selected, we must explicitly indicate LOCAL_RUNNER to avoid timeouts
  const runnerMetaFields = ['meta.jobRunnerFilter', 'meta.jobRunnerFilterType', 'meta.jobRunnerFilterMode'];
  
  // Track if we have any runner meta fields
  let hasRunnerFilter = false;
  let hasRunnerFilterType = false;
  let hasRunnerFilterMode = false;
  
  if (request.meta) {
    Object.keys(request.meta).forEach(key => {
      const value = request.meta![key];
      const metaKey = `meta.${key}`;
      if (runnerMetaFields.includes(metaKey)) {
        if (metaKey === 'meta.jobRunnerFilter') hasRunnerFilter = true;
        if (metaKey === 'meta.jobRunnerFilterType') hasRunnerFilterType = true;
        if (metaKey === 'meta.jobRunnerFilterMode') hasRunnerFilterMode = true;
        // Include runner fields if they have a non-empty value
        if (value !== undefined && value !== null && value !== '') {
          params[metaKey] = value;
        }
      } else {
        // For other meta fields, include all values except undefined and null
        if (value !== undefined && value !== null) {
          params[metaKey] = value;
        }
      }
    });
  }
  
  // Also check for any other flat meta.* fields in the request object
  Object.keys(request).forEach(key => {
    if (key.startsWith('meta.') && !params[key]) {
      const value = request[key];
      if (runnerMetaFields.includes(key)) {
        if (key === 'meta.jobRunnerFilter') hasRunnerFilter = true;
        if (key === 'meta.jobRunnerFilterType') hasRunnerFilterType = true;
        if (key === 'meta.jobRunnerFilterMode') hasRunnerFilterMode = true;
        // Include runner fields if they have a non-empty value
        if (value !== undefined && value !== null && value !== '') {
          params[key] = value;
        }
      } else {
        // For other meta fields, include all values except undefined and null
        if (value !== undefined && value !== null) {
          params[key] = value;
        }
      }
    }
  });
  
  // If runner meta fields are missing or empty, set LOCAL_RUNNER defaults
  // This prevents the server from waiting for runner reports that will never come
  // Default values match JobRunnerAdhocCommand.vue defaults:
  // - filter: "local" (but can be empty for LOCAL_RUNNER)
  // - runnerFilterType: "LOCAL_RUNNER"
  // - runnerFilterMode: "LOCAL"
  if (!hasRunnerFilterType || !params['meta.jobRunnerFilterType'] || params['meta.jobRunnerFilterType'] === '') {
    params['meta.jobRunnerFilterType'] = 'LOCAL_RUNNER';
  }
  if (!hasRunnerFilterMode || !params['meta.jobRunnerFilterMode'] || params['meta.jobRunnerFilterMode'] === '') {
    params['meta.jobRunnerFilterMode'] = 'LOCAL';
  }
  // jobRunnerFilter can be empty for LOCAL_RUNNER, so we only set it if it's explicitly provided
  
  // Log params for debugging (remove in production)
  if (process.env.NODE_ENV === 'development') {
    console.log('[adhocService] Sending request params:', params);
  }

  // Build URL with query parameters (matching original _genUrl behavior)
  // The original code uses _genUrl(appLinks.scheduledExecutionRunAdhocInline, data)
  // which appends serialized form data as query parameters to the URL
  // IMPORTANT: Include ALL parameters, even empty strings (meta fields can be empty)
  const queryParams = new URLSearchParams();
  Object.keys(params).forEach(key => {
    const value = params[key];
    // Include all values except undefined and null (empty strings are valid, especially for meta fields)
    if (value !== undefined && value !== null) {
      queryParams.append(key, String(value));
    }
  });
  
  const urlWithParams = `${url}?${queryParams.toString()}`;

  // Send as POST with query parameters in URL (matching original jQuery.ajax behavior)
  // The original code: jQuery.ajax({ type: 'POST', url: _genUrl(appLinks.scheduledExecutionRunAdhocInline, data) })
  // This sends POST with query params in URL, not in body
  const response = await api.post<RunAdhocResponse>(urlWithParams);

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

export interface RecentCommandExecution {
  href?: string;
  title?: string;
  execid?: string;
  filter?: string;
  extraMetadata?: any;
  status?: string;
  succeeded?: boolean;
}

export interface AdhocHistoryResponse {
  executions?: RecentCommandExecution[];
}

/**
 * Load recent adhoc command history
 * Uses: GET /execution/adhocHistoryAjax?project={project}&max={max}
 * Note: This is not an API endpoint, so we use axios directly with full URL
 * 
 * The original code uses: _genUrl(appLinks.adhocHistoryAjax, {max: self.loadMax})
 * The appLinks.adhocHistoryAjax URL already includes the project parameter from projParams,
 * so we use _genUrl to append max parameter correctly (using & if URL has ?, ? otherwise)
 */
export async function loadAdhocHistory(
  project: string,
  max: number = 20,
): Promise<AdhocHistoryResponse> {
  const rundeckContext = getRundeckContext();
  
  // Get appLinks - adhocHistoryAjax already includes project parameter from projParams
  // Format: /execution/adhocHistoryAjax?project={project}
  const appLinks = getAppLinks();
  const baseUrl = `${rundeckContext.rdBase}${appLinks.adhocHistoryAjax}`;
  
  // Use _genUrl to append max parameter correctly (matches original behavior)
  // _genUrl checks if URL has ? and uses & or ? accordingly
  const url = _genUrl(baseUrl, { max });

  // Use axios directly since this is not an API endpoint
  const response = await axios.get<AdhocHistoryResponse>(url, {
    headers: {
      "x-rundeck-ajax": "true",
      "Accept": "application/json",
    },
  });

  if (response.status !== 200) {
    throw new Error(`Failed to load adhoc history: ${response.statusText}`);
  }

  return response.data;
}
