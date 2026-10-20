import { RundeckBrowser } from "@rundeck/client";
import type { JobOptionsData } from "../types/jobs/JobEdit";
import { NavItem } from "../stores/NavBar";
import { AppMeta, RootStore } from "../stores/RootStore";
import { EventBus } from "../utilities/vueEventBus";

interface RundeckContextData extends Record<string, unknown> {
  otherData?: {
    uuid?: string;
    [key: string]: unknown;
  };
  features?: Record<string, boolean>;
  optionsData?: JobOptionsData;
}

export interface RundeckContext {
  eventBus: typeof EventBus;
  rdBase: string;
  apiVersion: string;
  projectName: string;
  activeTour: string;
  activeTourStep: string;
  appMeta: AppMeta;
  token: RundeckToken;
  tokens: {
    [key: string]: RundeckToken;
  };
  rundeckClient: RundeckBrowser;
  data: RundeckContextData;
  profile?: Record<string, unknown>;
  feature: { [key: string]: RundeckFeature };
  navbar: {
    items: Array<NavItem>;
  };
  rootStore: RootStore;
  locale?: string;
  language?: string;
}

export interface RundeckToken {
  TOKEN: string;
  URI: string;
}

export interface RundeckFeature {
  enabled: boolean;
}
