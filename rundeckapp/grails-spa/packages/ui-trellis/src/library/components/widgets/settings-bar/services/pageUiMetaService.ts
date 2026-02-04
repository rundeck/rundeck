import { loadJsonData } from "../../../../../app/utilities/loadJsonData";
import type { PageUiMeta } from "../NextUIIndicatorTypes";

const COOKIE_NAME = "nextUi";

export function getPageUiMeta(): PageUiMeta {
  return loadJsonData("pageUiMeta") || {};
}

export function getNextUiCapable(pageUiMeta: PageUiMeta): boolean {
  return (
    pageUiMeta.nextUiCapable === true ||
    pageUiMeta.nextUiCapable === "true"
  );
}

export function getNextUiSystemEnabled(
  pageUiMeta: PageUiMeta,
  hasNextUiCookie: boolean,
  isNextUiPage: boolean,
): boolean {
  let systemEnabled =
    pageUiMeta.nextUiSystemEnabled === true ||
    pageUiMeta.nextUiSystemEnabled === "true";

  if (hasNextUiCookie || isNextUiPage) {
    systemEnabled = true;
  }

  return systemEnabled;
}

export function hasNextUiCookie($cookies: any): boolean {
  return $cookies?.get(COOKIE_NAME) === "true";
}

export function isNextUiPage(pageUiMeta: PageUiMeta): boolean {
  return pageUiMeta.uiType === "next";
}
