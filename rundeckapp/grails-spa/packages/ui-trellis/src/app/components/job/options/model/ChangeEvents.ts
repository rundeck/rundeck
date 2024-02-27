import { JobOption } from "../../../../../library/types/jobs/JobEdit";

export enum Operation {
  Insert,
  Remove,
  Modify,
  Move,
}
export interface ChangeEvent {
  index: number;
  value?: JobOption;
  orig?: JobOption;
  dest?: number;
  operation: Operation;
  undo: Operation;
}
