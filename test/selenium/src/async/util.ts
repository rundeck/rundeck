import ReadLine from 'readline'
import {promisify} from 'util'

export function sleep(ms: number): Promise<{}> {
    return new Promise( res => {
        setTimeout(res, ms)
    })
}

export function prompt(prompt: string): Promise<string> {
    const rl = ReadLine.createInterface({
        input: process.stdin,
        output: process.stdout
    })

    return new Promise((res) => {
        rl.question(prompt, (answer) => res(answer))
    })
}

