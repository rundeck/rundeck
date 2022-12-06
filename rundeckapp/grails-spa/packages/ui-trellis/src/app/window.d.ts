import {RundeckContext} from '@/library'

declare global {
    interface Window {
        _rundeck: RundeckContext
        appLinks: {
            [key:string]: string
        }
    }
}