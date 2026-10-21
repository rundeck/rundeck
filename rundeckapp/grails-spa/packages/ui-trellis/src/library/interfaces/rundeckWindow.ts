import { RundeckBrowser } from "@rundeck/client";
import type { JobOptionsData } from "../types/jobs/JobEdit";
import { NavItem } from "../stores/NavBar";
import { AppMeta, RootStore } from "../stores/RootStore";
import { EventBus } from "../utilities/vueEventBus";

interface RundeckContextData extends Record<string, unknown> {
  activityPageHref?: string;
  activityUrl?: string;
  autorefreshms?: number;
  bulkDeleteUrl?: string;
  deleteExecAuth?: boolean;
  eventReadAuth?: boolean;
  executionModeActive?: boolean;
  filterOpts?: Record<string, unknown>;
  jobslistDateFormatMoment?: string;
  otherData?: {
    uuid?: string;
    [key: string]: unknown;
  };
  pagination?: {
    max?: number;
  };
  projectAdminAuth?: boolean;
  projectAclConfigPageUrl?: string;
  query?: Record<string, string>;
  runningOpts?: Record<string, boolean>;
  sinceUpdatedUrl?: string;
  systemAclConfigPageUrl?: string;
  viewOpts?: {
    showBulkDelete?: boolean;
  };
  features?: Record<string, boolean>;
  optionsData?: JobOptionsData;
  workflowData?: Record<string, unknown>;
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
  profile?: {
    username?: string;
    links?: {
      profile?: string;
      logout?: string;
    };
    [key: string]: unknown;
  };
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
