import { RundeckToken } from "../interfaces/rundeckWindow";
import type { AxiosResponseHeaders, RawAxiosResponseHeaders } from "axios";

type TokenHeaders = AxiosResponseHeaders | RawAxiosResponseHeaders;

export async function getUIAjaxTokens(): Promise<RundeckToken> {
  if (window._rundeck.token) {
    return window._rundeck.token;
  } else {
    const uiTokenElement = document.getElementById("ui_token");

    if (uiTokenElement) {
      const jsonText = uiTokenElement.textContent || uiTokenElement.innerText;
      const response =
        jsonText && jsonText !== "" ? JSON.parse(jsonText) : null;
      window._rundeck.token = response;
      return response;
    } else {
      throw new Error("No token exists");
    }
  }
}

export async function setNewUIToken(responseHeaders: TokenHeaders) {
  window._rundeck.token = {
    TOKEN: responseHeaders["x-rundeck-token-key"] as string,
    URI: responseHeaders["x-rundeck-token-uri"] as string,
  };
}

export async function getToken(token_name: string) {
  if (window._rundeck.tokens[token_name]) {
    return window._rundeck.tokens[token_name];
  } else {
    const uiTokenElement = document.getElementById(token_name);

    if (uiTokenElement) {
      const jsonText = uiTokenElement.textContent || uiTokenElement.innerText;
      const response =
        jsonText && jsonText !== "" ? JSON.parse(jsonText) : null;
      window._rundeck.tokens[token_name] = response;
      return response;
    } else {
      throw new Error("No token exists");
    }
  }
}

export async function setToken(
  responseHeaders: TokenHeaders,
  token_name: string,
) {
  window._rundeck.tokens[token_name] = {
    TOKEN: responseHeaders["x-rundeck-token-key"] as string,
    URI: responseHeaders["x-rundeck-token-uri"] as string,
  };
}

export default {
  getUIAjaxTokens,
  setNewUIToken,
  getToken,
  setToken,
};
