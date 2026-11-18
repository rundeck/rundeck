/**
 * Initial server-rendered model for the project export wait page.
 */
export interface ExportWaitData {
  token: string;
  project: string;
  ready: boolean;
  percentage: number;
  instance: string | null;
  iproject: string | null;
  pollUrl: string;
  downloadUrl: string;
  refreshUrl: string;
  returnUrl: string;
}

/**
 * JSON payload returned by polling GET /project/{project}/exportWait/{token}.json
 */
export interface ExportWaitPollResult {
  token?: string;
  ready?: boolean;
  percentage?: number;
  notFound?: boolean;
  errorMessage?: string | null;
  errors?: string[] | null;
  instance?: string | null;
}
