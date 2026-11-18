import { api } from "../../../../library/services/api";
import type { ExportWaitPollResult } from "../types/exportWaitTypes";

/**
 * Sentinel error message meaning the poll failed without a usable server
 * status text. Consumers map it to a translated message.
 */
export const EXPORT_WAIT_REQUEST_FAILED = "EXPORT_WAIT_REQUEST_FAILED";

/**
 * Poll the export wait endpoint for the current state of an async project export.
 *
 * The poll URL is a full application path (it already includes the context path),
 * so the API base URL of the shared client is overridden.
 *
 * @param pollUrl application path of the JSON exportWait endpoint
 * @returns the parsed poll result
 * @throws Error whose message is the HTTP status text, or
 *   EXPORT_WAIT_REQUEST_FAILED when there is none or the body is not a JSON object
 */
export async function pollExportWait(
  pollUrl: string,
): Promise<ExportWaitPollResult> {
  let response;
  try {
    response = await api.get(pollUrl, { baseURL: "" });
  } catch (e) {
    const err = e as { response?: { statusText?: string } };
    throw new Error(err?.response?.statusText || EXPORT_WAIT_REQUEST_FAILED);
  }
  if (response.status >= 400) {
    throw new Error(response.statusText || EXPORT_WAIT_REQUEST_FAILED);
  }
  const data = response.data;
  if (typeof data !== "object" || data === null || Array.isArray(data)) {
    throw new Error(EXPORT_WAIT_REQUEST_FAILED);
  }
  return data as ExportWaitPollResult;
}
