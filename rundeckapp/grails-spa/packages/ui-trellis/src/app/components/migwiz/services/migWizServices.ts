import { api } from "../../../../library/services/api";
import axios from "axios";

const BASE_URL_HACKWEEK = "https://api.rundeck.com/";
export async function getCredentials(formValues) {
  try {
    const response = await axios.post(
      `${BASE_URL_HACKWEEK}rba/get-credentials`,
      formValues,
      {
        validateStatus: function (status) {
          return status >= 200 && status < 500;
        },
      },
    );

    if (response.status !== 200 && response.status !== 400) {
      throw {
        message: `Error getting result: Response: ${response.status}`,
        response: response,
      };
    } else {
      return response.data;
    }
  } catch (e: any) {
    throw { message: "Error: " + e.message };
  }
}

export async function postStartInstance(token, formValues) {
  try {
    const response = await axios.post(
      `${BASE_URL_HACKWEEK}rba/new-trial`,
      formValues,
      {
        headers: { Authorization: `Bearer ${token}` },
        validateStatus: function (status) {
          return status >= 200 && status < 500;
        },
      },
    );

    if (response.status !== 200 && response.status !== 400) {
      throw {
        message: `Error getting result: Response: ${response.status}`,
        response: response,
      };
    } else {
      return response.data;
    }
  } catch (e) {
    throw { message: "Error: " + e.message };
  }
}

export async function postStartMigration(projectName, formValues) {
  try {
    const response = await api.post(
      `priv/migWiz/migrate/${projectName}`,
      formValues,
    );
    if (response.status === 200) {
      return { ok: true };
    } else {
      throw new Error("fail");
    }
  } catch (e) {
    throw { message: "Error: " + e.message };
  }
}
