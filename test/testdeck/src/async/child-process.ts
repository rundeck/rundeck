import * as CP from 'child_process'
import * as util from 'util'

export const exec = util.promisify(CP.exec)

export function spawn(command: string, args?: ReadonlyArray<string>, options?: CP.SpawnOptions): Promise<number> {
    return new Promise( res => {
        const childProcess = CP.spawn(command, args, options)

        childProcess.on('close', code => res(code))
    })
}