import {RundeckContext} from '@rundeck/ui-trellis'

declare global {
    interface Window {
        _rundeck: RundeckContext
        appLinks: {
            [key:string]: string
        }
    }
}