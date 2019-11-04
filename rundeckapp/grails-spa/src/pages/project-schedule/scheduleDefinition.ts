import { client } from '../../services/rundeckClient'

import {
  getRundeckContext,
  RundeckContext
} from "@rundeck/ui-trellis"

import axios from 'axios'

export interface ScheduleDefinition {
  id: number
  name : string
  description : string
  project : string
  minute : string
  hour : string
  dayOfMonth : string
  month : string
  dayOfWeek : string
  seconds : string
  year : string
  crontabString : string
  type : string
  scheduledExecutions: Object[]
}

export interface ScheduleSearchResult {
  schedules: ScheduleDefinition []
  maxRows: number
  offset: number
  totalRecords: number
}

export interface StandardResponse {
  messages: string[]
  success: boolean
}

export async function getAllProjectSchedules(offset: number, scheduleName: string): Promise<ScheduleSearchResult> {

  const rundeckContext = getRundeckContext()
  let offsetString = String(offset)
  const resp = await client.sendRequest({
    pathTemplate: '/projectSchedules/filteredProjectSchedules',
    queryParameters: {project: rundeckContext.projectName, offset: offsetString, name: scheduleName},
    baseUrl: rundeckContext.rdBase,
    method: 'GET'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error getting schedule definitions for project  ${rundeckContext.projectName}`)
  }
  else {
    return resp.parsedBody as ScheduleSearchResult
  }
}

export async function bulkDeleteSchedules(schedulesId: []): Promise<StandardResponse> {

  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: '/projectSchedules/massiveScheduleDelete',
    queryParameters: {project: rundeckContext.projectName},
    body: { schedulesId: schedulesId},
    baseUrl: rundeckContext.rdBase,
    method: 'POST'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error execution bulk delete for project  ${rundeckContext.projectName}`)
  } else {
    return resp.parsedBody as StandardResponse
  }
}

//TODO: fix this
export async function persistUploadedDefinition(formData: any): Promise<boolean> {
  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: '/projectSchedules/uploadFileDefinition',
    queryParameters: {project: rundeckContext.projectName},
    baseUrl: rundeckContext.rdBase,
    formData: formData,
    body: {scheduleUploadSelect : formData},
    method: 'POST',
    headers: {
      "x-rundeck-ajax": true,
      "Content-Type": "multipart/form-data"
    }
  })
  if (!resp.parsedBody) {
    throw new Error(`Error uploading schedule definitions for project  ${rundeckContext.projectName}`)
  }
  else {
    return true
  }
}

export async function persistUploadedDefinitions(formData: any){
  const rundeckContext = getRundeckContext()
  return axios({
    method: "post",
    headers: {
      "x-rundeck-ajax": true,
      "Content-Type": "multipart/form-data"
    },
    data: formData,
    params: { project: rundeckContext.projectName},
    url: `${window._rundeck.rdBase}projectSchedules/uploadFileDefinition`,
    withCredentials: true
  }).then(response => {
    if (response.data.errors) {
      return response.data.errors
    }else{
      return true
    }
  })
}
