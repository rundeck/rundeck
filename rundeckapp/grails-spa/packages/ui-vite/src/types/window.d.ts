import {RundeckContext} from '../interfaces/rundeckWindow'
import {AppLinks} from '../interfaces/AppLinks'

declare global {
    interface Window {
        _rundeck: RundeckContext
        appLinks: AppLinks
    }
}