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

import { api } from "../services/api";

const ServicesCache: { [svc: string]: { [provider: string]: any } } = {};
const ServiceProvidersCache: { [provider: string]: any } = {};

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
export const getPluginProvidersForService = async (svcName: string) => {
  if (ServicesCache[svcName]) {
    return new Promise((resolve, reject) => {
      resolve(ServicesCache[svcName]);
    });
  }
  const params = await getParameters();
  try {
    const resp = await api.get(`plugin/providers/${svcName}`);
    if (!resp.data) {
      throw new Error(`Error getting service providers list for ${svcName}`);
    } else {
      ServicesCache[svcName] = resp.data;
      return resp.data;
    }
  } catch (e) {
    return console.warn("Error getting service providers list", e);
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
    return new Promise((resolve, reject) => {
      resolve(ServiceProvidersCache[svcName].providers[provider]);
    });
  }
  const params = await getParameters();
  const qparams: any = {};
  if (params.project) {
    qparams.project = params.project;
  }

  const resp = await api.get(`plugin/detail/${svcName}/${provider}`, {
    params: qparams,
  });
  if (!resp.data) {
    throw new Error(
      `Error getting service provider detail for ${svcName}/${provider}`,
    );
  }

  if (!ServiceProvidersCache[svcName]) {
    ServiceProvidersCache[svcName] = {
      providers: {},
    };
  }
  ServiceProvidersCache[svcName].providers[provider] = resp.data;
  return resp.data;
};

export const validatePluginConfig = async (
  svcName: string,
  provider: string,
  config: any,
  ignoredScope: string | null = null,
) => {
  const params = await getParameters();
  const qparams = {} as { [key: string]: string };
  if (ignoredScope != null) {
    qparams["ignoredScope"] = ignoredScope;
  }

  const resp = await api.post(
    `plugin/validate/${svcName}/${provider}`,
    { config: config },
    { params: qparams },
  );
  if (!resp.data) {
    throw new Error(`Error validating plugin config for ${svcName}`);
  } else {
    return resp.data;
  }
};

export default {
  getPluginProvidersForService,
  getServiceProviderDescription,
  validatePluginConfig,
};
