export interface ISystemInfoResponse {
    system: ISystemInfo
}

export interface ISystemInfo {
    timestamp: {
        epoch: number,
        unit: string,
        datetime: string,
    }
    rundeck: {
        version: string,
        build: string,
        node: string,
        base: string,
        apiversion: number,
        serverUUID?: string,
    }
    executions: {
        active: boolean,
        executionMode: string,
    }
    os: {
        arch: string,
        name: string,
        version: string,
    }
    jvm: {
        name: string,
        vendor: string,
        version: string,
        implementation: string,
    }
    stats: {
        uptime: {
            duration: number,
            unit: string,
            since: {
                epoch: number,
                unit: string,
                datetime: string,
            },
        },
        cpu: {
            loadAverage: {
                unit: string,
                average: number,
            },
            processors: number,
        },
        memory: {
            unit: string,
            max: number,
            free: number,
            total: number,
        },
        scheduler: {
            running: number,
            threadPoolSize: number,
       },
       threads: {
           active: number,
       },
    },
    metrics: {
        href: string,
        contentType: string,
    },
    threadDump: {
        href: string,
        contentType: string,
    },
}