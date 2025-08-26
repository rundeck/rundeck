import { api } from "./api";

export interface ProjectListResponseItem {
  name?: string;
  description?: string;
  url?: string;
  label?: string;
  created?: string;
}

export async function listProjects(): Promise<Array<ProjectListResponseItem>> {
  let resp = await api.get("projects");
  if (resp.status !== 200) {
    throw { message: resp.data.message, response: resp };
  } else {
    return resp.data;
  }
}
