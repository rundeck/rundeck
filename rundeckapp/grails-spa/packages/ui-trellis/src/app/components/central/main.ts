import {
  getRundeckContext,
} from "../../../library";
import { EventBus } from "../../../library";

import { RootStore } from "../../../library/stores/RootStore";

const context = getRundeckContext();
context.eventBus = EventBus;
context.rootStore = new RootStore(context.appMeta);
