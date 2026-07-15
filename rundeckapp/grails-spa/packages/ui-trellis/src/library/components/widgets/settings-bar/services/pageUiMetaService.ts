import { loadJsonData } from "../../../../../app/utilities/loadJsonData";
import type { PageUiMeta } from "../NextUIIndicatorTypes";

export function getPageUiMeta(): PageUiMeta {
  const rawData = loadJsonData("pageUiMeta") || {};
  const nextUiCapable = rawData.nextUiCapable === true || rawData.nextUiCapable === "true";
  const uiType = rawData.uiType || "current";

  return {
    nextUiCapable: nextUiCapable || false,
    isNextUiPage: uiType === "next",
  };
}


