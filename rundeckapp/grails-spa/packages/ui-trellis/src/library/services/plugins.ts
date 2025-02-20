import { api } from "./api";

export async function getPluginDetail(serviceName: string, provider: string) {
  const resp = await api.get(`/plugin/detail/${serviceName}/${provider}`);
  if (resp.status !== 200) {
    throw { message: resp.data.message, response: resp };
  }
  return resp.data;
}
