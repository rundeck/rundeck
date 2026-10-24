import axios from "axios";
import { getAppLinks, getRundeckContext } from "../../../../library";
import type { Project } from "../types/projectTypes";

interface HomeSummary {
  execCount: number;
  totalFailedCount: number;
  recentProjects: string[];
  recentUsers: string[];
}

const serviceError = (error: unknown) => {
  const message =
    typeof error === "object" && error !== null && "message" in error
      ? error.message
      : undefined;
  const response =
    typeof error === "object" && error !== null && "response" in error
      ? error.response
      : undefined;
  return { message: "Error: " + String(message), response };
};

export async function getSummary(): Promise<HomeSummary> {
  const ctx = getRundeckContext();
  try {
    const response = await axios.get(
      `${ctx.rdBase}/api/${ctx.apiVersion}/home/summary`,
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
  } catch (e) {
    // e.message in this case is the error message from the server response
    throw serviceError(e);
  }
}

export async function getProjectNames(): Promise<string[]> {
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
  } catch (e) {
    // e.message in this case is the error message from the server response
    throw serviceError(e);
  }
}

export async function getProjects(): Promise<Project[]> {
  const ctx = getRundeckContext();
  try {
    const response = await axios.get(
      `${ctx.rdBase}/api/${ctx.apiVersion}/projects?meta=authz,config,message`,
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
  } catch (e) {
    // e.message in this case is the error message from the server response
    throw serviceError(e);
  }
}
