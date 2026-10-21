import { JobOption } from "../../../../../library/types/jobs/JobEdit";
import mitt, { Handler } from "mitt";

export enum Operation {
  Insert,
  Remove,
  Modify,
  Move,
}
export interface ChangeEvent<TValue = JobOption, TOperation = Operation> {
  index: number;
  value?: TValue;
  orig?: TValue;
  dest?: number;
  operation: TOperation;
  undo: TOperation;
}

type UndoRedoEventType =
  "undo" | "redo" | "revertAll" | "change" | "job-edit:edited";

export interface UndoRedoEventBus {
  on<T = unknown>(type: UndoRedoEventType, handler: Handler<T>): void;
  off<T = unknown>(type: UndoRedoEventType, handler?: Handler<T>): void;
  emit<T = undefined>(type: UndoRedoEventType, event?: T): void;
}

export function createUndoRedoEventBus(): UndoRedoEventBus {
  const emitter = mitt<Record<UndoRedoEventType, unknown>>();
  return {
    on: <T>(type: UndoRedoEventType, handler: Handler<T>) =>
      emitter.on(type, handler as Handler<unknown>),
    off: <T>(type: UndoRedoEventType, handler?: Handler<T>) =>
      emitter.off(type, handler as Handler<unknown> | undefined),
    emit: <T>(type: UndoRedoEventType, event?: T) => emitter.emit(type, event),
  };
}
