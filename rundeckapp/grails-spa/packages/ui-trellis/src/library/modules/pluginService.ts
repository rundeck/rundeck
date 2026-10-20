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

import { client } from "./rundeckClient";
import type PluginValidation from "../interfaces/PluginValidation";

export interface PluginProviderDescription {
  name: string;
  title: string;
  description?: string;
  configSet?: boolean;
  [key: string]: unknown;
}

export interface PluginProvidersResponse {
  service?: string;
  descriptions: PluginProviderDescription[];
  labels?: Record<string, string>;
}

const ServicesCache: Record<string, PluginProvidersResponse> = {};
const ServiceProvidersCache: Record<
  string,
  { providers: Record<string, unknown> }
> = {};

const getParameters = (): Promise<{ [key: string]: string }> => {
  return new Promise((resolve, reject) => {
    if (
      window._rundeck &&
      window._rundeck.rdBase &&
      window._rundeck.apiVersion
    ) {
      resolve({
        apiBase: `${window._rundeck.rdBase}/api/${window._rundeck.apiVersion}`,
        rdBase: window._rundeck.rdBase,
        project: window._rundeck.projectName,
      });
    } else {
      reject(new Error("No rdBase found"));
    }
  });
};
export const getPluginProvidersForService = async (
  svcName: string,
): Promise<PluginProvidersResponse | undefined> => {
  if (ServicesCache[svcName]) {
    return new Promise((resolve) => {
      resolve(ServicesCache[svcName]);
    });
  }
  const params = await getParameters();
  try {
    //TODO: convert endpoint to API
    const resp = await client.sendRequest({
      pathTemplate: `/plugin/providers/{svcName}`,
      pathParameters: { svcName: svcName },
      baseUrl: params.rdBase,
      method: "GET",
    });
    if (!resp.parsedBody) {
      throw new Error(`Error getting service providers list for ${svcName}`);
    } else {
      const body: PluginProvidersResponse = resp.parsedBody;
      ServicesCache[svcName] = body;
      return body;
    }
  } catch (e) {
    console.warn("Error getting service providers list", e);
    return undefined;
  }
};

export const getServiceProviderDescription = async (
  svcName: string,
  provider: string,
) => {
  if (
    ServiceProvidersCache[svcName] &&
    ServiceProvidersCache[svcName].providers[provider]
  ) {
    return new Promise((resolve) => {
      resolve(ServiceProvidersCache[svcName].providers[provider]);
    });
  }
  const params = await getParameters();
  const qparams: Record<string, string> = {};
  if (params.project) {
    qparams.project = params.project;
  }

  //TODO: convert endpoint to API
  const resp = await client.sendRequest({
    pathTemplate: `/plugin/detail/{svcName}/{provider}`,
    pathParameters: { svcName: svcName, provider: provider },
    baseUrl: params.apiBase,
    method: "GET",
    queryParameters: qparams,
  });
  if (!resp.parsedBody) {
    throw new Error(
      `Error getting service provider detail for ${svcName}/${provider}`,
    );
  }

  if (!ServiceProvidersCache[svcName]) {
    ServiceProvidersCache[svcName] = {
      providers: {},
    };
  }
  ServiceProvidersCache[svcName].providers[provider] = resp.parsedBody;
  return resp.parsedBody;
};

export const validatePluginConfig = async (
  svcName: string,
  provider: string,
  config: object,
  ignoredScope: string | null = null,
): Promise<PluginValidation> => {
  const params = await getParameters();
  const qparams = {} as { [key: string]: string };
  if (ignoredScope != null) {
    qparams["ignoredScope"] = ignoredScope;
  }

  //TODO: convert endpoint to API
  const resp = await client.sendRequest({
    pathTemplate: `/plugin/validate/{svcName}/{provider}`,
    pathParameters: { svcName: svcName, provider: provider },
    queryParameters: qparams,
    baseUrl: params.rdBase,
    method: "POST",
    body: { config: config },
  });
  if (!resp.parsedBody) {
    throw new Error(`Error getting service providers list for ${svcName}`);
  } else {
    return resp.parsedBody;
  }
};

export default {
  getPluginProvidersForService,
  getServiceProviderDescription,
  validatePluginConfig,
};
