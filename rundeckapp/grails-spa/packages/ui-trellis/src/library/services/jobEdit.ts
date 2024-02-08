import { JobOptionEdit, OptionValidation } from "../types/jobs/JobEdit";
import { api } from "./api";

export async function validateJobOption(
  project: string,
  option: JobOptionEdit,
): Promise<OptionValidation> {
  return api
    .post(`project/${project}/jobs/validateOption`, option)
    .then((r) => r.data)
    .catch((e) => {
      if (e.response && e.response.status === 400) {
        return e.response.data;
      } else {
        throw { message: e.response.data.message, response: e.response };
      }
    });
}
