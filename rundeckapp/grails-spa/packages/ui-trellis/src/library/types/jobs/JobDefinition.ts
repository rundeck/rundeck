export interface NodeFilters {
  filter: string;
  [key: string]: any;
}

export interface JobRef {
  group?: string;
  name?: string;
  uuid?: string;
  nodeStep: boolean;
}

export interface Command {
  description?: string;
  exec?: string;
  script?: string;
  scriptfile?: string;
  type?: string;
  configuration?: object;
  nodeStep?: boolean;
  args?: string;
  jobref?: JobRef;
}

export interface Sequence {
  commands: Command[];
  keepgoing: boolean;
  strategy: string;
}

export interface Option {
  name: string;
  label?: string;
  required?: boolean;
}

export interface JobBasic {
  name: string;
  description?: string;
  group?: string;
  nodeFilters?: NodeFilters;
  scheduleEnabled: boolean;
  executionEnabled: boolean;
  sequence?: Sequence;
  options?: Option[];
  notification?: Notification;
}

export enum LogLevel {
  Debug = "DEBUG",
  Verbose = "VERBOSE",
  Info = "INFO",
  Warn = "WARN",
  Error = "ERROR",
}

export interface ScheduleCronTab {
  crontab: string;
}

export interface ScheduleTime {
  time?: {
    hour?: string;
    minute?: string;
    seconds?: string;
  };
  month: string;
  year: string;
  day?: number;
  weekday?: {
    day: string;
  };
}

export interface JobDefinition extends JobBasic {
  defaultTab?: string;
  id: string;
  loglevel: LogLevel;
  nodeFilterEditable: boolean;
  nodesSelectedByDefault: boolean;
  plugins?: any;
  runnerSelector?: {
    runnerFilterMode: string;
    runnerFilterType: string;
  };
  schedule?: ScheduleCronTab | ScheduleTime;
  schedules?: any;
  uuid?: string;
  orchestrator?: string;
  configuration?: {
    attribute: string;
    sort: string;
  };
  type?: string;
}
