import axios, { AxiosResponse } from "axios";
import { _genUrl } from "../../../../utilities/genUrl";
import { getAppLinks, getRundeckContext } from "../../../../../library";

export async function getConfigMetaForExecutionMode(
  project: string,
  meta: string = "config",
): Promise<any> {
  let ctx = getRundeckContext();
  const response = await axios.request({
    method: "GET",
    headers: {
      "x-rundeck-ajax": "true",
    },
    url: `${ctx.rdBase}api/${ctx.apiVersion}/project/${project}/meta?meta=${meta}`,
    validateStatus(status) {
      return status <= 403;
    },
  });
  if (response.status >= 200 && response.status < 300) {
    return response.data;
  } else {
    throw {
      message: `Error fetching execution mode: ${response.status}`,
      response: response,
    };
  }
}
export async function getNodeSummary(): Promise<any> {
  try {
    const response = await axios.get(
      _genUrl(getAppLinks().frameworkNodeSummaryAjax, {}),
      {
        validateStatus(status) {
          return status <= 403;
        },
      },
    );
    if (response.status < 200 && response.status >= 300) {
      throw { message: "Error: " + response.status, response: response };
    }
    return response.data;
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}
export async function getNodes(params: any, url: string): Promise<any> {
  try {
    const response: AxiosResponse = await axios.request({
      method: "GET",
      headers: {
        "x-rundeck-ajax": "true",
      },
      url: _genUrl(url, params),
      validateStatus(status) {
        return status <= 403;
      },
    });

    const {
      status,
      data: { message },
    } = response;
    if (status >= 300) {
      if (message) {
        throw { message: message, response: response };
      } else {
        throw { message: "Error: " + status, response: response };
      }
    }

    return response.data;
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}
