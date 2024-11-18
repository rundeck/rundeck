import { api } from "../../../../library/services/api";
import axios from "axios";

const BASE_URL_HACKWEEK = "https://api.rundeck.com/";
export async function getCredentials(formValues) {
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

    // const response = {
    //   status: 200,
    //   data: {
    //     id: "test1",
    //     status: "PROVISIONING",
    //     subscription_number: null,
    //     end_date: "2024-12-15",
    //     instance_url: "https://test1.stg.runbook.pagerduty.cloud",
    //   },
    // };

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
