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
