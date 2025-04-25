import { api } from "./api";

export async function getPluginDetail(serviceName: string, provider: string) {
  let projectName: string = "";
  const params = await getParameters();

  if (params.project) {
    projectName = params.project;
  }
  console.log("getPluginDetail" + " params: ", params);
  const resp = await api.get(
    `/plugin/detail/${serviceName}/${provider}?project=${projectName}`,
  );
  if (resp.status !== 200) {
    throw { message: resp.data.message, response: resp };
  }
  return resp.data;
}

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
