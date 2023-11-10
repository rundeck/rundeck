import {api} from './api'
import {JobBrowseList, JobBrowseMeta} from '../types/jobs/JobBrowse'

export async function getProjectMeta(project: string, meta: string='*'): Promise<JobBrowseMeta[]> {
    const resp = await api.get(`project/${project}/meta?meta=${meta}`)
    if (resp.status !== 200) {
        throw {message: resp.data.message, response: resp}
    } else {
        return resp.data
    }
}

export async function browsePath(project: string, path: string,meta: string='*'): Promise<JobBrowseList> {
  const resp = await api.get(`project/${project}/jobs/browse/?path=${path}&meta=${meta}`)
  if (resp.status !== 200) {
    throw {message: resp.data.message, response: resp}
  } else {
    return resp.data
  }
}

export interface ResultItem {
    id: string;
    message?: string;
    errorCode?: string;
}

export interface BulkDeleteResponse {
    failures: ResultItem[];
    failedCount: number;
    successCount: number;
    requestCount: number;
    allsuccessful: boolean;
}

export interface BulkToggleResponse {
    failured: ResultItem[];
    succeeded: ResultItem[];
    enabled: boolean;
    requestCount: number;
    allsuccessful: boolean;
}

export async function bulkDeleteJobs(
    project: string,
    ids: string[]
): Promise<BulkDeleteResponse> {
    const resp = await api.post(`jobs/delete`, { ids });
    if (resp.status !== 200) {
        throw { message: resp.data.message, response: resp };
    } else {
        return resp.data;
    }
}
export async function bulkScheduleEnableDisable(
    project: string,
    ids: string[],
    enabled:boolean
): Promise<BulkToggleResponse> {
    const resp = await api.post(`jobs/schedule/${enabled?'enable':'disable'}`, { ids });
    if (resp.status !== 200) {
        throw { message: resp.data.message, response: resp };
    } else {
        return resp.data;
    }
}
export async function bulkExecutionEnableDisable(
    project: string,
    ids: string[],
    enabled:boolean
): Promise<BulkToggleResponse> {
    const resp = await api.post(`jobs/execution/${enabled?'enable':'disable'}`, { ids });
    if (resp.status !== 200) {
        throw { message: resp.data.message, response: resp };
    } else {
        return resp.data;
    }
}