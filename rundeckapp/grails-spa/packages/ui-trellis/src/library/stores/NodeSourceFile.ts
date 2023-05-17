import {observable} from 'mobx'
import {RundeckClient} from '@rundeck/client'
import {client} from '../../app/services/rundeckClient'
import {getRundeckContext} from '../rundeckService'
import {RootStore} from './RootStore'
import axios from 'axios'

export class NodeSourceFile {
  @observable index: number=-1
  @observable content: string=''
  @observable nodeSource: undefined | NodeSource = undefined

  mimeFormats: { [key: string]: string } = {
    'text/xml': 'xml',
    'application/xml': 'xml',
    'application/yaml': 'yaml',
    'text/yaml': 'yaml',
    'application/json': 'json',
  }
  constructor(readonly root: RootStore, readonly client: RundeckClient) {
  }

  async load() {
    this.nodeSource = await this.getProjectNodeSource(this.index)
  }

  async retrieveSourceContent() {
    this.content = await this.getProjectWriteableNodeSourceText(this.index, this.nodeSource!.resources!.syntaxMimeType!)
    return this.content
  }

  async storeSourceContent(text: string) {
    this.content = await this.saveProjectWriteableNodeSourceText(this.index, this.nodeSource!.resources!.syntaxMimeType!, text)
    return this.content
  }
  get modelFormat() {
      return this.mimeFormats[this.nodeSource!.resources!.syntaxMimeType!] || this.nodeSource!.resources!.syntaxMimeType! || ''
    return ''
  }

  async listProjectNodeSources(): Promise<NodeSource[]> {

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

  async getProjectNodeSource(index: number): Promise<NodeSource> {
    const response = await getRundeckContext().rundeckClient.sendRequest({
      method: 'GET',
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

  async getProjectWriteableNodeSourceText(index: number, mimeType: string): Promise<string> {
    let ctx = getRundeckContext()
    try {
      const response = await axios.get(
        `${ctx.rdBase}api/${ctx.apiVersion}/project/${ctx.projectName}/source/${index}/resources`,
        {
          responseType: 'text',
          headers: {
            'X-Rundeck-Ajax': 'true',
            accept: mimeType + ', text/*, */*'
          }
        })
      if (response.data) {
        return response.data
      } else if (response.status === 204) {
        return ''
      } else {
        throw new Error('request failed: ' + response)
      }
    } catch (e) {
      console.log('error', e)
      throw new Error('request error: ' + e)
    }
  }

  async saveProjectWriteableNodeSourceText(index: number, mimeType: string, content: string): Promise<string> {
    let ctx = getRundeckContext()
    try {
      const response = await axios.post(
        `${ctx.rdBase}api/${ctx.apiVersion}/project/${ctx.projectName}/source/${index}/resources`,
        content,
        {
          headers: {
            'X-Rundeck-Ajax': 'true',
            accept: mimeType,
            'Content-Type': mimeType,
            'X-RUNDECK-TOKEN-KEY': client.token,
            'X-RUNDECK-TOKEN-URI': client.uri
          }
        })
      if (response.data) {
        return response.data
      } else if (response.status === 204) {
        return ''
      } else {
        throw new Error('request failed: ' + response)
      }
    } catch (e) {
      console.log('error', e)
      throw new Error('request error: ' + e)
    }
  }

}

export interface NodeSourceResources {
  href: string
  editPermalink?: string
  writeable: boolean
  description?: string
  syntaxMimeType?: string
  empty?: boolean
}

export interface NodeSource {
  index: number
  type: string
  resources: NodeSourceResources
  errors?: string
}