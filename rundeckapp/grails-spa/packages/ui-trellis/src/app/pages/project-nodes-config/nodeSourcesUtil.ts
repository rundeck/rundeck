import { client } from "../../services/rundeckClient";

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
  const rundeckContext = getRundeckContext();
  const resp = await client.sendRequest({
    pathTemplate: "/api/{apiVersion}/project/{projectName}/sources",
    pathParameters: rundeckContext,
    baseUrl: rundeckContext.rdBase,
    method: "GET",
  });
  if (!resp.parsedBody) {
    throw new Error(
      `Error getting node sources list for ${rundeckContext.projectName}`,
    );
  } else {
    return resp.parsedBody as NodeSource[];
  }
}

export async function getProjectConfigurable(project: string, category: string) {
  const resp = await client.sendRequest({
    baseUrl: `${getRundeckContext().rdBase}api/${getRundeckContext().apiVersion}`,
    pathTemplate: "/project/" +project + "/configurable",
    method: "GET",
    queryParameters: {
        category: category,
        project: project
    }
  });

  if (resp.status === 200) {
    return {success: true, response: resp.parsedBody};
  } else {
    throw {success: false, message: resp.parsedBody.message};
  }
}
