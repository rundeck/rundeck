export interface PageUiMeta {
  nextUiCapable?: boolean | string;
  nextUiSystemEnabled?: boolean | string;
  uiType?: string;
}

export interface NextUIIndicatorData {
  nextUiCapable: boolean;
  nextUiSystemEnabled: boolean;
  nextUiEnabled: boolean;
}
