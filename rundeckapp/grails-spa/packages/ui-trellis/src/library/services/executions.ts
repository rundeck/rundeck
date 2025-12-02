import { Execution } from "../types/executions/Execution";
import { api } from "./api";

export interface PagedResult<T> {
  results: Array<T>;
  paging: PagingParams;
}

export interface PagingParams {
  max: number;
  offset: number;
  total?: number;
  count?: number;
}

export async function queryRunning(
  project: string,
  params: { [key: string]: string } = {},
  paging: PagingParams = { max: 0, offset: 0 },
): Promise<PagedResult<Execution>> {
  const qparams = { ...params };
  if (paging.offset > 0 || paging.max > 0) {
    qparams.max = paging.max.toString();
    qparams.offset = paging.offset.toString();
  }
  const resp = await api.get(`project/${project}/executions/running`, {
    params,
  });
  if (resp.status !== 200) {
    throw { message: resp.data.message, response: resp };
  }

  const results = resp.data.executions.map((e: any) => {
    const { "date-started": dateStarted, "date-ended": dateEnded } = e;
    return Object.assign({ dateStarted, dateEnded }, e) as Execution;
  });
  return { results, paging: resp.data.paging };
}

export async function getExecutions(
  project: string,
  query: any,
): Promise<PagedResult<Execution>> {
  try {
    const response = await api.get(`project/${project}/executions`, {
      params: query,
      validateStatus(status) {
        return status <= 403;
      },
    });

    if (response.status !== 200) {
      throw { message: response.data.message, response: response };
    }

    const results = response.data.executions.map((e: any) => {
      const { "date-started": dateStarted, "date-ended": dateEnded } = e;
      return Object.assign({ dateStarted, dateEnded }, e) as Execution;
    });
    return { results, paging: response.data.paging };
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}
