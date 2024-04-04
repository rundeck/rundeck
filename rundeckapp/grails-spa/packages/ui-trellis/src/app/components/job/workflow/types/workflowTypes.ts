export interface CommandData {
  label?: string;
  exec?: string;
  script?: string;
  scriptfile?: string;
  type?: string;
  config?: any;
}

export interface BasicData {
  keepgoing: boolean;
}

export interface StrategyData {
  strategy?: string;
  strategyConfig?: any;
}

export interface GlobalLogFiltersData {
  logFilter?: string;
}

export interface StepsData {
  commands: CommandData[];
}

export interface WorkflowData
  extends BasicData,
    StrategyData,
    GlobalLogFiltersData,
    StepsData {}

export function createBasicData({ keepgoing }): BasicData {
  return { keepgoing: !!keepgoing };
}

export function createStrategyData(
  { strategy },
  evalFunc: (val: string) => string = undefined,
): StrategyData {
  return { strategy: evalFunc ? evalFunc(strategy) : strategy };
}
export function createLogFiltersData({ logFilter }): GlobalLogFiltersData {
  return { logFilter };
}
export function createStepsData({ commands }): StepsData {
  return { commands };
}
