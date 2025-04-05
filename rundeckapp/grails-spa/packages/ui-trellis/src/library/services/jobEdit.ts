import { JobOptionEdit, OptionValidation, SaveJobResponse } from "../types/jobs/JobEdit";
import { api } from "./api";
import { _genUrl } from "../../app/utilities/genUrl";
import { getRundeckContext } from "../rundeckService";
import { JobDefinition } from "../types/jobs/JobDefinition";

const rundeckContext = getRundeckContext();

export async function validateJobOption(
  project: string,
  jobWasScheduled: boolean,
  option: JobOptionEdit,
): Promise<OptionValidation> {
  return api
    .post(`project/${project}/jobs/validateOption?jobWasScheduled=${jobWasScheduled}`, option)
    .then((r) => r.data)
    .catch((e) => {
      if (e.response && e.response.status === 400) {
        return e.response.data;
      } else {
        throw { message: e.response.data.message, response: e.response };
      }
    });
}

export async function getJobDefinition(jobId: string): Promise<any> {
  const resp = await api.get(`job/${jobId}`);
  if (resp.status === 200) {
    return resp.data;
  } else {
    throw {
      message: `Error getting result: Response: ${resp.status}`,
      response: resp,
    };
  }
}

export async function postJobDefinition(
  job: JobDefinition[] | any[],
  options: any = {
    dupeOption: "create",
    uuidOption: "remove",
  },
): Promise<SaveJobResponse> {
  const resp = await api.post(
    _genUrl(`/project/${rundeckContext.projectName}/jobs/import`, options),
    job,
  );
  if (resp.status === 200) {
    return resp.data;
  } else {
    throw new Error(`Error getting result: Response: ${resp.status}`);
  }
}
