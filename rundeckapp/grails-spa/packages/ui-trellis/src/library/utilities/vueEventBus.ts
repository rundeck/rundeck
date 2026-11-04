import mitt, { Emitter, EventType, Handler } from "mitt";

export interface DynamicEventBus {
  all: Emitter<Record<EventType, unknown>>["all"];
  on<T = unknown>(type: EventType, handler: Handler<T>): void;
  off<T = unknown>(type: EventType, handler?: Handler<T>): void;
  emit<T = undefined>(type: EventType, event?: T): void;
}

const emitter = mitt<Record<EventType, unknown>>();
export const EventBus: DynamicEventBus = {
  all: emitter.all,
  on: <T>(type: EventType, handler: Handler<T>) =>
    emitter.on(type, handler as Handler<unknown>),
  off: <T>(type: EventType, handler?: Handler<T>) =>
    emitter.off(type, handler as Handler<unknown> | undefined),
  emit: <T>(type: EventType, event?: T) => emitter.emit(type, event),
};
