import {RundeckContext} from './interfaces/rundeckWindow'
import {AppLinks} from "./interfaces/AppLinks";
import {defineComponent} from "vue";

declare global {
    const Component: ReturnType<typeof defineComponent>;
    interface Window {
        _rundeck: RundeckContext
        appLinks: AppLinks
        ProWebhookComponents: Component[]
    }
}