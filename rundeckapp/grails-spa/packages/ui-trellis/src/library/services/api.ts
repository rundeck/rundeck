import { getRundeckContext } from "../rundeckService";
import axios from "axios";

const rundeckContext = getRundeckContext();
let token = { URI: "", TOKEN: "" };
if (document && document.getElementById("web_ui_token")) {
  let elementById = document.getElementById("web_ui_token");
  token = JSON.parse(elementById!.textContent!);
}

export const api = axios.create({
  baseURL: rundeckContext.rdBase + "api/" + rundeckContext.apiVersion + "/",
  headers: {
    "X-Rundeck-ajax": "true",
    Accept: "application/json",
  },
  validateStatus: function (status) {
    return status >= 200 && status < 500;
  },
});

api.interceptors.request.use((config) => {
  config.headers["Accept"] = "application/json";
  if (config.method !== "get") {
    config.headers["X-RUNDECK-TOKEN-URI"] = token.URI!;
    config.headers["X-RUNDECK-TOKEN-KEY"] = token.TOKEN!;
  }
  return config;
});
api.interceptors.response.use((resp) => {
  if (
    resp.headers["x-rundeck-token-key"] &&
    resp.headers["x-rundeck-token-uri"]
  ) {
    token.TOKEN = resp.headers["x-rundeck-token-key"];
    token.URI = resp.headers["x-rundeck-token-uri"];
  }
  return resp;
});