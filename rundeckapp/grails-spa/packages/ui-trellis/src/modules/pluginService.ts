/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {client} from './rundeckClient'

const ServicesCache : { [svc: string]: { [provider: string]: any } }= {}
const ServiceProvidersCache: { [provider: string]: any } = {}

const getParameters = () :Promise<{[key:string]:string}>=> {
  return new Promise((resolve, reject) => {
    if (window._rundeck && window._rundeck.rdBase && window._rundeck.apiVersion) {
      resolve({
        apiBase: `${window._rundeck.rdBase}/api/${window._rundeck.apiVersion}`,
        rdBase: window._rundeck.rdBase
      })
    } else {
      reject(new Error('No rdBase found'))
    }
  })
}
export const getPluginProvidersForService = async (svcName:string) => {
  if (ServicesCache[svcName]) {
    return new Promise((resolve, reject) => {
      resolve(ServicesCache[svcName])
    })
  }
  const params=await getParameters()
  try {
    const resp = await client.sendRequest({
      pathTemplate: `/plugin/providers/{svcName}`,
      pathParameters: {svcName: svcName},
      baseUrl: params.rdBase,
      method: 'GET'
    });
    if (!resp.parsedBody) {
      throw new Error(`Error getting service providers list for ${svcName}`);
    }
    else {
      ServicesCache[svcName] = resp.parsedBody;
      return resp.parsedBody;
    }
  }
  catch (e) {
    return console.warn('Error getting service providers list', e);
  }
}

export const getServiceProviderDescription = async (svcName:string, provider:string) => {
  if (ServiceProvidersCache[svcName] && ServiceProvidersCache[svcName].providers[provider]) {
    return new Promise((resolve, reject) => {
      resolve(ServiceProvidersCache[svcName].providers[provider])
    })
  }
  const params=await getParameters()

  const resp = await client.sendRequest({
    pathTemplate: `/plugin/detail/{svcName}/{provider}`,
    pathParameters: {svcName:svcName,provider:provider},
    baseUrl: params.rdBase,
    method: 'GET'
  });
  if (!resp.parsedBody) {
    throw new Error(`Error getting service provider detail for ${svcName}/${provider}`);
  }

  if (!ServiceProvidersCache[svcName]) {
    ServiceProvidersCache[svcName] = {
      providers: {}
    };
  }
  ServiceProvidersCache[svcName].providers[provider] = resp.parsedBody;
  return resp.parsedBody;

}


export const validatePluginConfig = async (svcName:string, provider:string, config:any) => {
  const params = await getParameters()
  const resp = await client.sendRequest({
    pathTemplate: `/plugin/validate/{svcName}/{provider}`,
    pathParameters: {svcName:svcName,provider:provider},
    baseUrl: params.rdBase,
    method: 'POST',
    body: {config: config}
  })
  if (!resp.parsedBody) {
    throw new Error(`Error getting service providers list for ${svcName}`)
  } else {
    return resp.parsedBody
  }
}

export default {
  getPluginProvidersForService,
  getServiceProviderDescription,
  validatePluginConfig
}
