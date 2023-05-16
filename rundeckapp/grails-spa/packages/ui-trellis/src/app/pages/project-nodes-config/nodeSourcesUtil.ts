import { client } from '../../services/rundeckClient'

import {
  getRundeckContext,
  RundeckContext
} from '../../../library'
import axios from 'axios'

export interface NodeSourceResources {
  href: string
  editPermalink?: string
  writeable: boolean
  description?: string
  syntaxMimeType?:string
  empty?: boolean
}
export interface NodeSource {
  index: number
  type: string
  resources: NodeSourceResources
  errors?:string
}
export interface StorageAccess{
  authorized: boolean;
  action: string;
  description: string;
}
export async function getProjectNodeSources(): Promise<NodeSource[]> {

  const rundeckContext = getRundeckContext()
  const resp = await client.sendRequest({
    pathTemplate: '/api/{apiVersion}/project/{projectName}/sources',
    pathParameters: rundeckContext,
    baseUrl: rundeckContext.rdBase,
    method: 'GET'
  })
  if (!resp.parsedBody) {
    throw new Error(`Error getting node sources list for ${rundeckContext.projectName}`)
  } else {
    return resp.parsedBody as NodeSource[]
  }
}

export async function getProjectNodeSource(index: number): Promise<NodeSource> {
  const response = await getRundeckContext().rundeckClient.sendRequest({
    method: 'get',
    pathTemplate: '/api/{apiVersion}/project/{projectName}/source/{index}',
    baseUrl: getRundeckContext().rdBase,
    pathParameters: {
      ...getRundeckContext(),
      index: index.toString()
    }
  })
  if (response.parsedBody) {
    return response.parsedBody as NodeSource
  } else {
    throw new Error('request failed: ' + response)
  }

}

export async function getProjectWriteableNodeSourceText(index: number, mimeType: string): Promise<String> {
  let ctx = getRundeckContext()
  try {
    const response = await axios.get(
      `${ctx.rdBase}api/${ctx.apiVersion}/project/${ctx.projectName}/source/${index}/resources`,
      {
        responseType: 'text',
        headers: {
          'X-Rundeck-Ajax': 'true',
          accept: mimeType+', text/*, */*'
        }
      })
    if (response.data) {
      return response.data
    } else if(response.status===204) {
      return ''
    }else{
      throw new Error('request failed: ' + response)
    }
  }catch(e){
    console.log("error",e)
    throw new Error('request error: ' + e)
  }
}

export async function saveProjectWriteableNodeSourceText(index: number, mimeType: string, content:string): Promise<String> {
  let ctx = getRundeckContext()
  try {
    const response = await axios.post(
      `${ctx.rdBase}api/${ctx.apiVersion}/project/${ctx.projectName}/source/${index}/resources`,
      content,
      {
        headers: {
          'X-Rundeck-Ajax': 'true',
          accept: mimeType,
          'Content-Type':mimeType,
          "X-RUNDECK-TOKEN-KEY": client.token,
          "X-RUNDECK-TOKEN-URI": client.uri
        }
      })
    if (response.data) {
      return response.data
    } else if(response.status===204) {
      return ''
    }else{
      throw new Error('request failed: ' + response)
    }
  }catch(e){
    console.log("error",e)
    throw new Error('request error: ' + e)
  }
}
