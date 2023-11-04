import {api} from './api'
import {JobBrowseList} from '../types/jobs/JobBrowse'

export async function browsePath(project: string, path: string): Promise<JobBrowseList> {
  const resp = await api.get(`project/${project}/jobs/browse/?path=${path}`)
  if (resp.status !== 200) {
    throw {message: resp.data.message, response: resp}
  } else {
    return resp.data
  }
}