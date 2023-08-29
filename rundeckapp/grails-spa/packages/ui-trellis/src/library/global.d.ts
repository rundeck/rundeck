import type { RundeckContext} from "./interfaces/rundeckWindow";
import type { AppLinks } from "./interfaces/AppLinks";

declare global {
    interface Window {
        _rundeck: RundeckContext
        appLinks: AppLinks
    }
}