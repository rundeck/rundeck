export interface DateFilterProps {
  enabled: boolean;
  datetime: string;
}
export type ComponentStub = boolean | { template: string };
export type Stubs = string[] | Record<string, ComponentStub>;
export interface Directive {
  beforeMount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  mounted?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUpdate?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  updated?: (el: HTMLElement, binding: any, vnode: any, prevVnode: any) => void;
  beforeUnmount?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
  unmounted?: (
    el: HTMLElement,
    binding: any,
    vnode: any,
    prevVnode: any,
  ) => void;
}
export interface GlobalOptions {
  stubs?: Stubs;
  mocks?: Record<string, unknown>;
  directives?: Record<string, Directive>;
}
export interface Report {
  execution: {
    id: string;
    permalink: string;
  };
  status: string;
  dateCompleted: string;
  node: {
    total: number;
    succeeded: number;
    failed: number;
  };
}
export type Reports = Report[];
export interface RundeckContext {
  eventBus: typeof import("../../../../library").EventBus;
  rdBase: string;
  apiVersion: string;
  projectName: string;
  activeTour: string;
  activeStep: string;
  activeTourStep: string;
  appMeta: Record<string, unknown>;
  token: { TOKEN: string; URI: string };
  tokens: { default: { TOKEN: string; URI: string } };
  rundeckClient: import("@rundeck/client").RundeckBrowser;
  data: {
    jobslistDateFormatMoment: string;
    projectAdminAuth: boolean;
    deleteExecAuth: boolean;
    activityUrl: string;
    nowrunningUrl: string;
    bulkDeleteUrl: string;
    activityPageHref: string;
    sinceUpdatedUrl: string;
    autorefreshms: number;
    pagination: { max: number };
    filterOpts: Record<string, unknown>;
    query: Record<string, unknown>;
    runningOpts: { allowAutoRefresh: boolean; loadRunning: boolean };
    viewOpts: { showBulkDelete: boolean };
  };
  feature: Record<string, { enabled: boolean }>;
  navbar: { items: Array<import("../../../../library/stores/NavBar").NavItem> };
  rootStore: import("../../../../library/stores/RootStore").RootStore;
}
