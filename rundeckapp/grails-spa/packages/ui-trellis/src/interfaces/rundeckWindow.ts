export interface RundeckContext {
    rdBase: string
    apiVersion: string
    projectName: string
    activeTour: string
    activeTourStep: string
    token: RundeckToken
    tokens: {
        [key:string]: RundeckToken
    }
}

export interface RundeckToken {
    TOKEN: string
    URI: string
}