import mitt, {Emitter, EventType} from "mitt";

const emitter = mitt()
export const EventBus : Emitter<Record<EventType, any>> = emitter
