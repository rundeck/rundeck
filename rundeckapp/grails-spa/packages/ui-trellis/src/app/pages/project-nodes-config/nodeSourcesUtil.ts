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
  const rdBase = (window as any)?._rundeck?.rdBase?.replace(/\/+$/, "") ?? "";
  const apiVersion = (window as any)?._rundeck?.apiVersion ?? "53";
  const url = `${rdBase}/api/${apiVersion}/project/${encodeURIComponent(
    project,
  )}/configurable?category=${encodeURIComponent(category)}`;

  const needsCrossOrigin = (() => {
    try {
      return new URL(rdBase).origin !== window.location.origin;
    } catch {
      return false;
    }
  })();
  const creds: RequestCredentials = needsCrossOrigin
    ? "include"
    : "same-origin";

  async function touchSession() {
    try {
      await fetch(`${rdBase}/api/${apiVersion}/system/info`, {
        method: "GET",
        credentials: creds,
        headers: {
          Accept: "application/json",
          "X-Rundeck-Requested-By": "rundeck-ui",
        },
      });
    } catch {}
  }

  async function doPost() {
    const resp = await fetch(url, {
      method: "POST",
      credentials: creds,
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        "X-Rundeck-Requested-By": "rundeck-ui",
        // If you dev with a token, uncomment:
        // "X-Rundeck-Auth-Token": "<your-dev-token>",
      },
      body: JSON.stringify({ extraConfig }), // keep your current payload shape
    });

    if (!resp.ok) {
      const text = await resp.text().catch(() => "");
      const err: any = new Error(
        `HTTP ${resp.status} ${resp.statusText}${text ? ` - ${text}` : ""}`,
      );
      err.status = resp.status;
      err.body = text;
      throw err;
    }
    return resp.json().catch(() => ({}));
  }

  try {
    const data = await doPost();
    const ok = data?.result?.success === true;
    if (!ok)
      throw new Error(
        data?.errors || data?.message || "Unknown error from API",
      );
    return { success: true, response: true };
  } catch (e: any) {
    if (e?.status === 401) {
      await touchSession(); // mimic “navigate away”
      const data = await doPost(); // retry once
      const ok = data?.result?.success === true;
      if (ok) return { success: true, response: true };
    }
    throw {
      success: false,
      status: e?.status,
      message: e?.message || e?.body || "Request failed",
    };
  }
}
