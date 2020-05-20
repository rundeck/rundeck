import {RundeckContext} from '../interfaces/rundeckWindow'

declare global {
    interface Window {
        _rundeck: RundeckContext
        appLinks: {
            [key:string]: string
        }
    }
}