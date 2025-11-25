import axios from "axios";
import { getRundeckContext } from "../../../../library";

export async function getActivity(query: any): Promise<any> {
  const ctx = getRundeckContext();
  try {
    const response = await axios.get(
      `${ctx.rdBase}api/${ctx.apiVersion}/project/${ctx.projectName}/executions`,
      {
        method: "GET",
        headers: {
          "x-rundeck-ajax": "true",
        },
        params: query,
        validateStatus(status) {
          return status <= 403;
        },
      },
    );

    if (response.status >= 200 && response.status < 300) {
      return response.data;
    } else {
      // intentionally throw an error to be caught locally
      throw { message: response.status, response: response };
    }
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}
