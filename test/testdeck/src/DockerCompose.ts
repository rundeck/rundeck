import CP from 'child_process'

import readline from 'readline'

interface IConfig {
    env?: {[k:string]: string}
}

export class DockerCompose {
    workDir: string

    constructor(readonly worDir: string, readonly config: IConfig) {}

    async containers(): Promise<String[]> {
        const cp = CP.spawn('docker-compose', ['ps'], {cwd: this.worDir})

        const stdout = (async () => {
            let output = [] as String[]
            let burnedHeader = false
            const rl = readline.createInterface(cp.stdout)
            for await (let l of rl) {
                if (burnedHeader)
                    output.push(l.split(/\s+/)[0])
                if (l.startsWith('----'))
                    burnedHeader = true
            }
            return output
        })()

        return stdout
    }

    async stop(service: string): Promise<void> {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['stop', service], {cwd: this.worDir, stdio: 'ignore', env})

        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }

    async start(service: string): Promise<void> {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['start', service], {cwd: this.worDir, stdio: 'ignore', env})

        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }
}
