import { api } from "../../../../library/services/api";

const BASE_URL_HACKWEEK = "https://api.rundeck.com/";
export async function getCredentials(formValues) {
  // const response = await api.post(
  //   `${BASE_URL_HACKWEEK}rba/get-credentials`,
  //   formValues,
  //   {
  //     headers: {
  //       Accept: "*/*",
  //     },
  //   },
  // );

  const response = {
    status: 200,
    data: {
      message: "Trial credentials created successfully",
      access_token:
        "eyJraWQiOiJRSko4cEFjbjE3eXdtOCthdHMxeXVPb2t4bjF5XC81aG1VNUFXUDFsdVVCND0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiIzNDFmZDFqdXIxM281YnZiZTB2cjlzcGw2diIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoidXNlci1zZXJ2aWNlXC9yZWFkIiwiYXV0aF90aW1lIjoxNzMxNzAyNjc0LCJpc3MiOiJodHRwczpcL1wvY29nbml0by1pZHAudXMtd2VzdC0yLmFtYXpvbmF3cy5jb21cL3VzLXdlc3QtMl9MeVRJenFrd0EiLCJleHAiOjE3MzE3MDYyNzQsImlhdCI6MTczMTcwMjY3NCwidmVyc2lvbiI6MiwianRpIjoiNTNlZGViNzgtN2QyNi00MmM2LWJkNTMtYTRmM2U5Yjk3M2M1IiwiY2xpZW50X2lkIjoiMzQxZmQxanVyMTNvNWJ2YmUwdnI5c3BsNnYifQ.K7cEjmBgUvhDf3uLSBmXLqV0wx7vDIJFyf7o_XM0MiZtat4h2o6QHWz2U7crCoPz050I5eJ3RPeZlSmkA-FsK3_Vm3t_k3xvlB0BVSqbr4o9TnFAKKtRdp-9W_0q7th-JxlrkchQ7HTKnaHOyTBvN7oHgGg8t9xdMXylCbQfE1tCdOTMca4qb7zku7oB8ZoeWmC-5Qt60BfSeEEkDJYA4t2HOwyTSZubzJsOABu4yQimYHgOTFbdyhHMsXWzN8I3U0zKFZHjwz0MEuJxXBumjokNIol7QA4iNgEAD3B6YjHQS0XAXGtIXI6yljeToMllYAwedHmsMmRQNMXT2pD44g",
      expires_in: 3600,
      token_type: "Bearer",
    },
  };
  if (response.status !== 200) {
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
    // const response = await api.post(
    //   `${BASE_URL_HACKWEEK}rba/new-trial`,
    //   formValues,
    //   {
    //      headers: { Authorization: `Bearer ${token}` }
    //   },
    // );
    const response = {
      status: 200,
      data: {
        id: "test1",
        status: "PROVISIONING",
        subscription_number: null,
        end_date: "2024-12-15",
        instance_url: "https://test1.stg.runbook.pagerduty.cloud",
      },
    };
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
