import CP from 'child_process'

import readline from 'readline'

interface IConfig {
    env?: {[k:string]: string}
    composeFileName: string
}

export class DockerCompose {

    constructor(readonly workDir: string, readonly config: IConfig) {}

    async containers(): Promise<String[]> {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['-f', this.config.composeFileName, 'ps'], {cwd: this.workDir, env})

        const stdout = (async () => {
            let output = [] as String[]
            let burnedHeader = false
            const rl = readline.createInterface(cp.stdout)
            for await (let l of rl) {
                if (burnedHeader) {
                    output.push(l.split(/\s+/)[0])
                }else if (l.startsWith('----')) {
                    burnedHeader = true
                }else if(l.startsWith('NAME ')){
                    burnedHeader = true
                }
            }
            return output
        })()

        return stdout
    }

    async up(service?: string) {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'up', '-d', '--build'], {cwd: this.workDir, stdio: 'inherit', env})

        await new Promise<void>((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }

    async down(service?: string) {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'down'], {cwd: this.workDir, stdio: 'inherit', env})

        await new Promise<void>((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }

    async stop(service?: string): Promise<void> {
        const env = {...process.env, ...this.config.env || {}}

        const args = ['-f', this.config.composeFileName, 'stop']

        if (service)
            args.push(service)

        const cp = CP.spawn('docker-compose', args, {cwd: this.workDir, stdio: 'inherit', env})

        await new Promise<void>((res, rej) => {
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

        const cp = CP.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'start', service], {cwd: this.workDir, stdio: 'ignore', env})

        await new Promise<void>((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }

    async logs(service?: string): Promise<void> {
        const env = {...process.env, ...this.config.env || {}}

        const cp = CP.spawn('docker-compose', ['--compatibility','-f', this.config.composeFileName, 'logs'], {cwd: this.workDir, stdio: 'inherit', env})

        await new Promise<void>((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code)
                else
                    res()
            })
        })
    }
}
