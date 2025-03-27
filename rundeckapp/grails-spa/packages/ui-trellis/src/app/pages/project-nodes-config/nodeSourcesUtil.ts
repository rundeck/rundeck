import { api } from "../../../library/services/api";

import { getRundeckContext } from "../../../library";

export interface NodeSourceResources {
  href: string;
  editPermalink?: string;
  writeable: boolean;
  description?: string;
  syntaxMimeType?: string;
  empty?: boolean;
}
export interface NodeSource {
  index: number;
  type: string;
  resources: NodeSourceResources;
  errors?: string;
}
export interface StorageAccess {
  authorized: boolean;
  action: string;
  description: string;
}
export async function getProjectNodeSources(): Promise<NodeSource[]> {
  const response = await api.get(
    `/project/${getRundeckContext().projectName}/sources`,
  );
  if (response.data) {
    return response.data as NodeSource[];
  } else {
    throw new Error(
      `Error getting node sources list for ${getRundeckContext().projectName}`,
    );
  }
}

export async function getProjectConfigurable(
  project: string,
  category: string,
) {
  const response = await api.get(`/project/${project}/configurable`, {
    params: { category },
  });

  if (response.status === 200) {
    return { success: true, response: response.data };
  } else {
    throw { success: false, message: response.data.message };
  }
}

export async function setProjectConfigurable(
  project: string,
  category: string,
  extraConfig: any,
) {
  const response = await api.post(
    `/project/${project}/configurable`,
    {
      extraConfig,
    },
    {
      params: { category },
    },
  );

  if (response.data.result?.success) {
    return { success: true, response: response.data.result.success };
  } else {
    throw { success: false, message: response.data.errors };
  }
}
