import { Execution } from "../types/executions/Execution";
import { api } from "./api";

export interface PagedResult<T> {
  results: Array<T>;
  paging: PagingParams;
}

export interface PagingParams {
  max: number;
  offset: number;
}

export async function queryRunning(
  project: string,
  params: { [key: string]: string } = {},
  paging: PagingParams = { max: 0, offset: 0 },
): Promise<PagedResult<Execution>> {
  let qparams = { ...params };
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
    const { "date-started": dateStarted, "date-completed": dateCompleted } = e;
    return Object.assign({ dateStarted, dateCompleted }, e) as Execution;
  });
  return { results, paging: resp.data.paging };
}
