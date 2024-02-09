import axios from "axios";
import { getAppLinks, getRundeckContext } from "../../../../library";

export async function getSummary(): Promise<any> {
  const ctx = getRundeckContext();
  try {
    const response = await axios.get(
      `${ctx.rdBase}api/${ctx.apiVersion}/home/summary`,
      {
        method: "GET",
        headers: {
          "x-rundeck-ajax": "true",
        },
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

export async function getProjectNames(): Promise<any> {
  const appLinks = getAppLinks();
  try {
    const response = await axios.get(appLinks.menuProjectNamesAjax, {
      method: "GET",
      headers: {
        "x-rundeck-ajax": "true",
      },
      params: {
        format: "json",
      },
      validateStatus(status) {
        return status <= 403;
      },
    });

    if (response.status >= 200 && response.status < 300) {
      return response.data.projectNames;
    } else {
      // intentionally throw an error to be caught locally
      throw { message: response.status, response: response };
    }
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}

export async function getProjects(): Promise<any> {
  const ctx = getRundeckContext();
  try {
    const response = await axios.get(
      `${ctx.rdBase}api/${ctx.apiVersion}/projects?meta=authz,config,message`,
      {
        method: "GET",
        headers: {
          "x-rundeck-ajax": "true",
        },
        validateStatus(status) {
          return status <= 403;
        },
      },
    );

    if (response.status >= 200 && response.status < 300) {
      return response.data;
    } else {
      // intentionally throw an error to be caught locally
      throw { message: `Error: ${response.status}`, response: response };
    }
  } catch (e: any) {
    // e.message in this case is the error message from the server response
    throw { message: "Error: " + e.message, response: e.response };
  }
}
