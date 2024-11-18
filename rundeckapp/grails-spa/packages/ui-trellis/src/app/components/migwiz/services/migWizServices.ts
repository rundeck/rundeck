import { api } from "../../../../library/services/api";
import axios from "axios";

const BASE_URL_HACKWEEK = "https://api.rundeck.com/";
export async function getCredentials(formValues) {
  const response = await axios.post(
    `${BASE_URL_HACKWEEK}rba/get-credentials`,
    formValues,
  );

  if (response.status !== 200 && response.status !== 400) {
    throw {
      message: `Error getting result: Response: ${response.status}`,
      response: response,
    };
  } else {
    return response.data;
  }
}

export async function postStartInstance(token, formValues) {
  try {
    const response = await axios.post(
      `${BASE_URL_HACKWEEK}rba/new-trial`,
      formValues,
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    );

    if (response.status === 200) {
      return response.data;
    }
  } catch (e) {
    console.log(e);
  }
}

export async function postStartMigration(projectName, formValues) {
  const response = await api.post(
    `priv/migWiz/migrate/${projectName}`,
    formValues,
  );
  if (response.status === 200) {
    return { ok: true };
  } else {
    throw new Error("fail");
  }
}
