import { JobOption } from "../../../../../library/types/jobs/JobEdit";
import type { Emitter } from "mitt";

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

export type UndoRedoEvents = {
  undo: ChangeEvent;
  redo: ChangeEvent;
  revertAll: void;
  change: ChangeEvent;
  "job-edit:edited": boolean;
};

export type UndoRedoEventBus = Emitter<UndoRedoEvents>;
